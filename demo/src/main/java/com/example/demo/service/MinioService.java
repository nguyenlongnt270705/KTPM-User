package com.example.demo.service;

import io.minio.*;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * Uploads a file to MinIO storage.
     *
     * @param file   The multipart file to upload.
     * @param folder The folder path within the bucket (e.g., "movies", "thumbnails").
     * @return The object name (path) of the uploaded file in MinIO.
     * @throws MinioException           if MinIO operation fails.
     * @throws IOException              if file I/O operation fails.
     * @throws InvalidKeyException      if invalid credentials.
     * @throws NoSuchAlgorithmException if algorithm is not available.
     */
    public String uploadFile(MultipartFile file, String folder) throws MinioException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        String fileName = generateFileName(file.getOriginalFilename());
        String objectName = folder + "/" + fileName;

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        log.info("File uploaded successfully: {}", objectName);
        return objectName;
    }

    /**
     * Downloads a file from MinIO storage.
     *
     * @param objectName The object name (path) of the file in MinIO.
     * @return InputStream of the file content.
     */
    public InputStream downloadFile(String objectName) throws MinioException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }

    /** Deletes a file from MinIO storage. */
    public void deleteFile(String objectName) throws MinioException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
        log.info("File deleted successfully: {}", objectName);
    }

    /**
     * Checks if a file exists in MinIO storage.
     *
     * @param objectName The object name (path) of the file to check.
     * @return true if the file exists, false otherwise.
     */
    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generates a unique file name using UUID to avoid naming conflicts.
     *
     * @param originalFileName The original file name.
     * @return A unique file name with UUID prefix.
     */
    private String generateFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * Gets the full URL of a file in MinIO.
     * This can be used to construct public URLs for accessing files.
     *
     * @param objectName The object name (path) of the file.
     * @return The full URL to access the file.
     */
    public String getFileUrl(String objectName) {
        // Có thể cấu hình thêm base URL trong application.properties nếu cần
        // Hiện tại trả về object name, có thể build URL đầy đủ sau
        return objectName;
    }
}

