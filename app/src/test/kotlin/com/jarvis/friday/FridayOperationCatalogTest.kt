package com.jarvis.friday

import com.google.common.truth.Truth.assertThat
import com.jarvis.friday.core.FridayOperationCatalog
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
        assertThat(
            FridayOperationCatalog.operations
                .filter { it.domain == com.jarvis.friday.core.FridayOperationSpec.Domain.SECURITY }
                .all { it.existingTool?.startsWith("security.") == true }
        ).isTrue()
    }
}
