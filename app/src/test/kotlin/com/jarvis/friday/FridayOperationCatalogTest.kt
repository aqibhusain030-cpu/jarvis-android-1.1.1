package com.jarvis.friday

import com.google.common.truth.Truth.assertThat
import com.jarvis.friday.core.FridayOperationCatalog
import com.jarvis.friday.core.FridayOperationSpec
import org.junit.Test

class FridayOperationCatalogTest {
    @Test
    fun catalog_ids_are_unique() {
        val ids = FridayOperationCatalog.operations.map { it.id }
        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun high_risk_operations_have_explicit_tools() {
        FridayOperationCatalog.operations
            .filter { it.risk.name == "HIGH" && it.supported }
            .forEach { assertThat(it.existingTool).isNotNull() }
    }

    @Test
    fun security_operations_are_explicitly_scoped() {
        val securityOperations = FridayOperationCatalog.operations.filter {
            it.domain == FridayOperationSpec.Domain.SECURITY
        }

        assertThat(securityOperations).isNotEmpty()
        for (operation in securityOperations) {
            val tool = operation.existingTool
            assertThat(tool).isNotNull()
            // Web security analysis has its own capability namespace and checks
            // SecurityScopeRegistry authorization in NetworkSecurityBackend.
            if (operation.id == "websec.analyze") {
                assertThat(tool).isEqualTo("websec.analyze")
            } else {
                assertThat(operation.id).startsWith("security.")
                assertThat(tool).startsWith("security.")
            }
        }
    }
}
