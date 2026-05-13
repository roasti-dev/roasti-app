package dev.roasti.common

import arrow.core.EitherNel
import dev.roasti.common.api.FieldError

typealias ValidationResult<T> = EitherNel<FieldError, T>
