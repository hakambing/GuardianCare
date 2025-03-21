package com.example.guardiancare.data.api

import com.example.guardiancare.data.models.ElderlyResponse
import com.example.guardiancare.data.models.CheckInResponse
import com.example.guardiancare.data.models.AssignElderlyRequest
import com.example.guardiancare.data.models.AssignElderlyResponse
import com.example.guardiancare.data.models.UnassignElderlyRequest
import com.example.guardiancare.data.models.UnassignElderlyResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ElderlyApiService {
    @GET("/api/elderly/caretaker/{caretakerId}")
    suspend fun getCaretakerElderly(
        @Header("Authorization") token: String,
        @Path("caretakerId") caretakerId: String
    ): Response<List<ElderlyResponse>>
    
    @GET("/api/elderly/check-in/{elderlyId}/latest")
    suspend fun getLatestCheckIn(
        @Header("Authorization") token: String,
        @Path("elderlyId") elderlyId: String
    ): Response<CheckInResponse>
    
    @GET("/api/elderly/check-in/{elderlyId}")
    suspend fun getElderlyCheckIns(
        @Header("Authorization") token: String,
        @Path("elderlyId") elderlyId: String
    ): Response<List<CheckInResponse>>
    
    @POST("/api/elderly/assign")
    suspend fun assignElderlyByEmail(
        @Header("Authorization") token: String,
        @Body request: AssignElderlyRequest
    ): Response<AssignElderlyResponse>
    
    @POST("/api/elderly/remove-caretaker")
    suspend fun unassignElderly(
        @Header("Authorization") token: String,
        @Body request: UnassignElderlyRequest
    ): Response<UnassignElderlyResponse>
    
    @Multipart
    @POST("/api/check-in/mobile/audio")
    suspend fun uploadAudioForTranscription(
        @Header("Authorization") token: String,
        @Part audioFile: MultipartBody.Part
    ): Response<CheckInResponse>

    @POST("/api/check-in/mobile/text")
    suspend fun uploadTextCheckIn(
        @Header("Authorization") token: String,
        @Body text: RequestBody
    ): Response<CheckInResponse>
}
