package com.jarvis.capability
import kotlinx.serialization.Serializable
@Serializable data class CapabilityDescriptor(
 val id:String,val displayName:String,val backend:String,val operations:List<String>,
 val executableTools:List<String> = emptyList(),val enabled:Boolean=true,val requiresPlatformIntegration:Boolean=false
)
