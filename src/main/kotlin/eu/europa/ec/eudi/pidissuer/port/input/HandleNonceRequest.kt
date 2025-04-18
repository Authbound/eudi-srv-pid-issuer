/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.europa.ec.eudi.pidissuer.port.input

import eu.europa.ec.eudi.pidissuer.port.out.credential.GenerateCNonce
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Clock
import java.time.Duration

/**
 * Response to a Nonce Request.
 */
@Serializable
data class NonceResponseTO(
    @Required @SerialName("c_nonce") val cNonce: String,
)

/**
 * Handles a CNonce Request.
 */
internal class HandleNonceRequest(
    private val clock: Clock,
    private val cNonceExpiresIn: Duration,
    private val generateCNonce: GenerateCNonce,
) {
    suspend operator fun invoke(): NonceResponseTO = NonceResponseTO(generateCNonce(clock.instant(), cNonceExpiresIn))
}
