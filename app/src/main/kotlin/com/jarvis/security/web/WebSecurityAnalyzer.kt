package com.jarvis.security.web

import com.jarvis.friday.security.SecurityRisk
import com.jarvis.friday.security.SecurityScopeRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class WebFinding(
    val id: String,
    val severity: String,
    val title: String,
    val evidence: String,
    val remediation: String
)

data class WebSecurityAssessment(
    val url: String,
    val statusCode: Int,
    val findings: List<WebFinding>
)

@Singleton
class WebSecurityAnalyzer @Inject constructor(
    private val scopes: SecurityScopeRegistry
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    suspend fun analyze(scopeId: String, url: String): WebSecurityAssessment = withContext(Dispatchers.IO) {
        val uri = URI(url)
        require(uri.scheme.equals("https", true))
        val host = uri.host ?: error("URL host required")
        check(scopes.isAuthorized(scopeId, host, "websec_analyze", SecurityRisk.R3_VALIDATION))
        val response = client.newCall(Request.Builder().url(url).get().build()).execute()
        response.use {
            val findings = mutableListOf<WebFinding>()
            val headers = it.headers
            if (headers["Strict-Transport-Security"].isNullOrBlank()) {
                findings += WebFinding("WEB-TLS-001", "MEDIUM", "HSTS header missing", "Strict-Transport-Security was not observed.", "Enable HSTS with an appropriate max-age after validating deployment.")
            }
            if (headers["Content-Security-Policy"].isNullOrBlank()) {
                findings += WebFinding("WEB-CSP-001", "MEDIUM", "Content-Security-Policy missing", "No CSP header was observed.", "Deploy a restrictive CSP appropriate to the application.")
            }
            if (!headers["X-Content-Type-Options"].orEmpty().equals("nosniff", true)) {
                findings += WebFinding("WEB-HEAD-001", "LOW", "X-Content-Type-Options missing or weak", "The response did not advertise nosniff.", "Set X-Content-Type-Options: nosniff.")
            }
            if (headers["Referrer-Policy"].isNullOrBlank()) {
                findings += WebFinding("WEB-HEAD-002", "LOW", "Referrer-Policy missing", "No Referrer-Policy header was observed.", "Set an explicit restrictive Referrer-Policy.")
            }
            headers.values("Set-Cookie").forEach { cookie ->
                val lower = cookie.lowercase()
                if (!lower.contains("; secure")) findings += WebFinding("WEB-COOKIE-001", "MEDIUM", "Cookie without Secure attribute", "A Set-Cookie response omitted Secure.", "Mark sensitive cookies Secure.")
                if (!lower.contains("httponly")) findings += WebFinding("WEB-COOKIE-002", "MEDIUM", "Cookie without HttpOnly attribute", "A Set-Cookie response omitted HttpOnly.", "Mark session cookies HttpOnly where JavaScript access is unnecessary.")
            }
            WebSecurityAssessment(url, it.code, findings)
        }
    }
}
