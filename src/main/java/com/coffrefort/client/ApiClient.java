package com.coffrefort.client;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.coffrefort.client.model.FileEntry;
import com.coffrefort.client.model.NodeItem;
import com.coffrefort.client.model.Quota;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Client d'API pour communiquer avec le backend.
 * Gère l'authentification JWT et les appels HTTP.
 */
public class ApiClient {
    private String baseUrl = "http://localhost:8888/"; // URL de base de l'API
    private String authToken; // JWT token
    
    private final OkHttpClient httpClient;
    private final ObjectMapper jsonMapper;

    public ApiClient() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
        this.jsonMapper = new ObjectMapper();
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Authentifie l'utilisateur et récupère le token JWT
     * @param email Email de l'utilisateur
     * @param password Mot de passe
     * @return true si la connexion réussit, false sinon
     * @throws IOException En cas d'erreur réseau
     */
    public boolean login(String email, String password) throws IOException {
        // Créer le corps de la requête JSON
        String jsonBody = jsonMapper.writeValueAsString(new LoginRequest(email, password));
        
        RequestBody body = RequestBody.create(
            jsonBody,
            MediaType.parse("application/json")
        );
        
        Request request = new Request.Builder()
            .url(baseUrl + "/auth/login")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Échec de la connexion: " + response.code() + " - " + errorBody);
            }
            
            // Parser la réponse JSON
            String responseBody = response.body().string();
            JsonNode jsonResponse = jsonMapper.readTree(responseBody);
            
            // Extraire le token de la réponse
            if (jsonResponse.has("token")) {
                this.authToken = jsonResponse.get("token").asText();
                return true;
            } else {
                throw new IOException("Réponse invalide: token manquant");
            }
        }
    }

    public boolean isAuthenticated() {
        return authToken != null && !authToken.isEmpty();
    }

    public String getAuthToken() {
        return authToken;
    }

    public void clearToken() {
        this.authToken = null;
    }

    /**
     * Effectue une déconnexion (invalide le token côté serveur si l'API le supporte)
     */
    public void logout() throws IOException {
        if (!isAuthenticated()) {
            return;
        }
        
        // TODO: Si l'API a un endpoint de déconnexion, l'appeler ici
        // Request request = new Request.Builder()
        //     .url(baseUrl + "/auth/logout")
        //     .post(RequestBody.create("", null))
        //     .addHeader("Authorization", "Bearer " + authToken)
        //     .build();
        // httpClient.newCall(request).execute();
        
        // Nettoyer le token localement
        clearToken();
    }

    /**
     * Retourne une arborescence factice de dossiers/fichiers avec versions.
     * TODO: Remplacer par un vrai appel API
     */
    public List<NodeItem> listRoot() {
        List<NodeItem> root = new ArrayList<>();
        NodeItem docs = NodeItem.folder("Documents")
                .withFiles(List.of(
                        FileEntry.of("CV.pdf", 128_000, Instant.now().minusSeconds(86_400), 3),
                        FileEntry.of("Lettre_motivation.docx", 64_000, Instant.now().minusSeconds(123_000), 5)
                ));
        NodeItem photos = NodeItem.folder("Photos")
                .addChild(NodeItem.folder("Vacances 2024").withFiles(List.of(
                        FileEntry.of("plage.jpg", 2_048_000, Instant.now().minusSeconds(55_000), 1),
                        FileEntry.of("coucher_soleil.jpg", 1_648_000, Instant.now().minusSeconds(45_000), 2)
                )))
                .addChild(NodeItem.folder("Famille"));

        NodeItem racineFichiers = NodeItem.folder("Racine");
        racineFichiers.getFiles().add(FileEntry.of("todo.txt", 1_024, Instant.now().minusSeconds(3_600), 8));

        root.add(docs);
        root.add(photos);
        root.add(racineFichiers);
        return root;
    }

    /**
     * Quota simulé: 2 Go max, 350 Mo utilisés.
     * TODO: Remplacer par un vrai appel API
     */
    public Quota getQuota() {
        return new Quota(350L * 1024 * 1024, 2L * 1024 * 1024 * 1024);
    }

    /**
     * Classe interne pour la requête de login
     */
    private static class LoginRequest { 
        public String email;
        public String password;

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }
}