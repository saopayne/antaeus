package io.pleo.antaeus.core.exceptions

abstract class EntityNotFoundException(entity: String, id: String) : Exception("$entity '$id' was not found")