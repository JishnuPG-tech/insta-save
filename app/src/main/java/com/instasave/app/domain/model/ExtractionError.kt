package com.instasave.app.domain.model

sealed interface ExtractionError {
    data object InvalidUrl : ExtractionError
    data object LoginRequired : ExtractionError
    data class RateLimited(val retryAfterSec: Long? = null) : ExtractionError
    data object NotFound : ExtractionError
    data object PrivateAccount : ExtractionError
    data class ParserOutdated(val engine: EngineTag, val detail: String) : ExtractionError
    data object Network : ExtractionError
    data class Unknown(val detail: String) : ExtractionError
}
