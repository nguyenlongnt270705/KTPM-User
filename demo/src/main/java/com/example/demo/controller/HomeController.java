package com.example.demo.controller;

import com.example.demo.dto.movie.HomeResponseDTO;
import com.example.demo.exceptions.ErrorMessage;
import com.example.demo.exceptions.ResponseObject;
import com.example.demo.service.MovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private final MovieService movieService;

    @GetMapping("/home")
    public ResponseEntity<ResponseObject<HomeResponseDTO>> getHomeData() {
        try {
            HomeResponseDTO homeData = movieService.getHomeData();
            return ResponseEntity.ok(ResponseObject.success(homeData));
        } catch (Exception e) {
            log.error("Error getting home data", e);
            return ResponseEntity.internalServerError()
                    .body(ResponseObject.error(ErrorMessage.UNHANDLED_ERROR, e.getMessage()));
        }
    }
}

