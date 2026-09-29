package com.jarvis.capability
import javax.inject.Inject
import javax.inject.Singleton
@Singleton class CapabilityRegistry @Inject constructor(backends:Set<@JvmSuppressWildcards CapabilityBackend>) {
 private val index:Map<String,CapabilityDescriptor> = buildMap {
  backends.flatMap{it.descriptors()}.forEach{ d -> require(d.id.isNotBlank()); require(put(d.id,d)==null){"Duplicate capability id"} }
 }
 fun all()=index.values.sortedBy{it.id}
 fun get(id:String)=index[id]
 fun findOperation(operation:String)=all().firstOrNull{operation in it.operations}
}
