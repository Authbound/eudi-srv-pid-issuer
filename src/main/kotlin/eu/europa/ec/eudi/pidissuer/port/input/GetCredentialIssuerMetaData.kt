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

import arrow.core.Option
import arrow.core.none
import arrow.core.some
import eu.europa.ec.eudi.pidissuer.domain.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

class GetCredentialIssuerMetaData(private val credentialIssuerMetaData: CredentialIssuerMetaData) {
    operator fun invoke(): CredentialIssuerMetaDataTO = credentialIssuerMetaData.toTransferObject()
}

@Serializable
data class CredentialIssuerMetaDataTO(
    @Required @SerialName("credential_issuer")
    val credentialIssuer: String,
    @SerialName("authorization_servers")
    val authorizationServers: List<String>? = null,
    @Required @SerialName("credential_endpoint")
    val credentialEndpoint: String,
    @SerialName("deferred_credential_endpoint")
    val deferredCredentialEndpoint: String? = null,
    @SerialName("notification_endpoint")
    val notificationEndpoint: String? = null,
    @SerialName("credential_response_encryption")
    val credentialResponseEncryption: CredentialResponseEncryptionTO? = null,
    @SerialName("batch_credential_issuance")
    val batchCredentialIssuance: BatchCredentialIssuanceTO? = null,
    @SerialName("credential_identifiers_supported")
    val credentialIdentifiersSupported: Boolean? = null,
    @SerialName("signed_metadata")
    val signedMetadata: String? = null,
    @SerialName("display")
    val display: List<DisplayTO>? = null,
    @Required @SerialName("credential_configurations_supported")
    val credentialConfigurationsSupported: JsonObject,
) {

    @Serializable
    data class CredentialResponseEncryptionTO(
        @Required @SerialName("alg_values_supported")
        val encryptionAlgorithms: List<String>,
        @Required @SerialName("enc_values_supported")
        val encryptionMethods: List<String>,
        @Required @SerialName("encryption_required")
        val required: Boolean,
    )

    @Serializable
    data class BatchCredentialIssuanceTO(
        @Required @SerialName("batch_size") val batchSize: Int,
    )
}

@Serializable
data class DisplayTO(
    @SerialName("name")
    val name: String? = null,
    @SerialName("locale")
    val locale: String? = null,
    @SerialName("logo")
    val logo: LogoTO? = null,
) {
    @Serializable
    data class LogoTO(
        @Required @SerialName("uri")
        val uri: String,
        @SerialName("alt_text")
        val alternativeText: String? = null,
    )
}

private fun CredentialIssuerMetaData.toTransferObject(): CredentialIssuerMetaDataTO = CredentialIssuerMetaDataTO(
    credentialIssuer = id.externalForm,
    authorizationServers = authorizationServers.map { it.externalForm },
    credentialEndpoint = credentialEndPoint.externalForm,
    deferredCredentialEndpoint = deferredCredentialEndpoint?.externalForm,
    notificationEndpoint = notificationEndpoint?.externalForm,
    credentialResponseEncryption = credentialResponseEncryption.toTransferObject().getOrNull(),
    batchCredentialIssuance = when (batchCredentialIssuance) {
        BatchCredentialIssuance.NotSupported -> null
        is BatchCredentialIssuance.Supported -> CredentialIssuerMetaDataTO.BatchCredentialIssuanceTO(batchCredentialIssuance.batchSize)
    },
    credentialIdentifiersSupported = true,
    signedMetadata = null,
    display = display.map { it.toTransferObject() }.takeIf { it.isNotEmpty() },
    credentialConfigurationsSupported = JsonObject(
        credentialConfigurationsSupported.associate { it.id.value to credentialMetaDataJson(it, batchCredentialIssuance) },
    ),
)

private fun CredentialResponseEncryption.toTransferObject(): Option<CredentialIssuerMetaDataTO.CredentialResponseEncryptionTO> =
    fold(
        ifNotSupported = none(),
        ifOptional = { optional ->
            CredentialIssuerMetaDataTO.CredentialResponseEncryptionTO(
                encryptionAlgorithms = optional.parameters.algorithmsSupported.map { it.name },
                encryptionMethods = optional.parameters.methodsSupported.map { it.name },
                required = false,
            ).some()
        },
        ifRequired = { required ->
            CredentialIssuerMetaDataTO.CredentialResponseEncryptionTO(
                encryptionAlgorithms = required.parameters.algorithmsSupported.map { it.name },
                encryptionMethods = required.parameters.methodsSupported.map { it.name },
                required = true,
            ).some()
        },
    )

private fun CredentialIssuerDisplay.toTransferObject(): DisplayTO =
    DisplayTO(
        name = name,
        locale = locale?.toString(),
        logo = logo?.toTransferObject(),
    )

private fun ImageUri.toTransferObject(): DisplayTO.LogoTO =
    DisplayTO.LogoTO(
        uri = uri.toString(),
        alternativeText = alternativeText,
    )

private fun CredentialConfiguration.format(): Format = when (this) {
    is JwtVcJsonCredentialConfiguration -> JWT_VS_JSON_FORMAT
    is MsoMdocCredentialConfiguration -> MSO_MDOC_FORMAT
    is SdJwtVcCredentialConfiguration -> SD_JWT_VC_FORMAT
}

@OptIn(ExperimentalSerializationApi::class)
private fun credentialMetaDataJson(
    d: CredentialConfiguration,
    batchCredentialIssuance: BatchCredentialIssuance,
): JsonObject = buildJsonObject {
    put("format", d.format().value)
    d.scope?.value?.let { put("scope", it) }
    d.cryptographicBindingMethodsSupported.takeIf { it.isNotEmpty() }
        ?.let { cryptographicBindingMethodsSupported ->
            putJsonArray("cryptographic_binding_methods_supported") {
                addAll(cryptographicBindingMethodsSupported.map { it.methodName() })
            }
        }
    d.credentialSigningAlgorithmsSupported.takeIf { it.isNotEmpty() }
        ?.let { credentialSigningAlgorithmsSupported ->
            putJsonArray("credential_signing_alg_values_supported") {
                addAll(credentialSigningAlgorithmsSupported.map { it.name })
            }
        }
    d.proofTypesSupported.takeIf { it != ProofTypesSupported.Empty }
        ?.let { proofTypesSupported ->
            putJsonObject("proof_types_supported") {
                proofTypesSupported.values.forEach {
                    put(it.proofTypeName(), it.toJsonObject())
                }
            }
        }
    when (d) {
        is JwtVcJsonCredentialConfiguration -> TODO()
        is MsoMdocCredentialConfiguration -> d.toTransferObject(batchCredentialIssuance)(this)
        is SdJwtVcCredentialConfiguration -> d.toTransferObject()(this)
    }
}

private fun CryptographicBindingMethod.methodName(): String =
    when (this) {
        is CryptographicBindingMethod.Jwk -> "jwk"
        is CryptographicBindingMethod.CoseKey -> "cose_key"
        is CryptographicBindingMethod.DidMethod -> "did:$didMethod"
        is CryptographicBindingMethod.DidAnyMethod -> "DID"
    }

private fun ProofType.proofTypeName(): String =
    when (this) {
        is ProofType.Jwt -> "jwt"
    }

@OptIn(ExperimentalSerializationApi::class)
private fun ProofType.toJsonObject(): JsonObject =
    buildJsonObject {
        when (this@toJsonObject) {
            is ProofType.Jwt -> {
                putJsonArray("proof_signing_alg_values_supported") {
                    addAll(signingAlgorithmsSupported.map { it.name })
                }
            }
        }
    }

@OptIn(ExperimentalSerializationApi::class)
internal fun MsoMdocCredentialConfiguration.toTransferObject(
    batchCredentialIssuance: BatchCredentialIssuance,
): JsonObjectBuilder.() -> Unit = {
    put("doctype", docType)
    if (display.isNotEmpty()) {
        putJsonArray("display") {
            addAll(display.map { it.toTransferObject() })
        }
    }
    if (policy != null) {
        putJsonObject("policy") {
            put("one_time_use", policy.oneTimeUse)
            if (batchCredentialIssuance is BatchCredentialIssuance.Supported) {
                put("batch_size", batchCredentialIssuance.batchSize)
            }
        }
    }

    putJsonObject("claims") {
        msoClaims.forEach { (nameSpace, attributes) ->
            putJsonObject(nameSpace) {
                attributes.forEach { attribute -> attribute.toTransferObject(this) }
            }
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal fun SdJwtVcCredentialConfiguration.toTransferObject(): JsonObjectBuilder.() -> Unit = {
    if (display.isNotEmpty()) {
        putJsonArray("display") {
            addAll(display.map { it.toTransferObject() })
        }
    }
    put("vct", type.value)
    putJsonObject("claims") {
        claims.forEach { attribute -> attribute.toTransferObject(this) }
    }
}

internal fun CredentialDisplay.toTransferObject(): JsonObject = buildJsonObject {
    put("name", name.name)
    put("locale", name.locale.toString())
    logo?.let { logo ->
        putJsonObject("logo") {
            put("uri", logo.uri.toString())
            logo.alternativeText?.let { put("alt_text", it) }
        }
    }
    description?.let { put("description", it) }
    backgroundColor?.let { put("background_color", it) }
    backgroundImage?.let { backgroundImage ->
        putJsonObject("background_image") {
            put("uri", backgroundImage.uri.toString())
            backgroundImage.alternativeText?.let { put("alt_text", it) }
        }
    }
    textColor?.let { put("text_color", it) }
}

internal val AttributeDetails.toTransferObject: JsonObjectBuilder.() -> Unit
    get() = {
        putJsonObject(name) {
            put("mandatory", mandatory)
            valueType?.let { put("value_type", it) }
            if (display.isNotEmpty()) {
                put("display", display.toTransferObject())
            }
        }
    }

internal fun Display.toTransferObject(): JsonArray =
    map { (locale, value) ->
        buildJsonObject {
            put("name", value)
            put("locale", locale.toString())
        }
    }.run { JsonArray(this) }
