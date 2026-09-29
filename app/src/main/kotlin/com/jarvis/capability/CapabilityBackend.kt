package com.jarvis.capability
interface CapabilityBackend { val id:String; fun descriptors():List<CapabilityDescriptor> }
