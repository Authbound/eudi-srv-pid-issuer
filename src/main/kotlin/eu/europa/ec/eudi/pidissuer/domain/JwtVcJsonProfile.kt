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
package eu.europa.ec.eudi.pidissuer.domain

import com.nimbusds.jose.JWSAlgorithm

private const val JWT_VS_JSON_FORMAT = "jwt_vc_json"

/**
 * W3C VC signed as a JWT, not using JSON-LD (jwt_vc_json)
 */
data class JwtVcJsonMetaData(
    override val scope: Scope? = null,
    val cryptographicSuitesSupported: List<JWSAlgorithm>,
    override val display: List<CredentialDisplay>,
) : CredentialMetaData {
    override val cryptographicBindingMethodsSupported: List<CryptographicBindingMethod>
        get() = listOf(CryptographicBindingMethod.Mso(cryptographicSuitesSupported))

    override val format: Format = Format(JWT_VS_JSON_FORMAT)
}

object Dummy2
