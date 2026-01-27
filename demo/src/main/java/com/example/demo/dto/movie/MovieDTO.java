package com.example.demo.dto.movie;

import com.example.demo.domain.Movie;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long movieId;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String posterUrl;
    private String trailerUrl;
    private String videoUrl;
    private Integer duration;
    private LocalDate releaseDate;
    private Double rating;
    private Long viewCount;
    private String status;
    private CategoryDTO category;

    public static MovieDTO fromEntity(Movie movie) {
        if (movie == null) {
            return null;
        }
        return MovieDTO.builder()
                .movieId(movie.getMovieId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .thumbnailUrl(movie.getThumbnailUrl())
                .posterUrl(movie.getPosterUrl())
                .trailerUrl(movie.getTrailerUrl())
                .videoUrl(movie.getVideoUrl())
                .duration(movie.getDuration())
                .releaseDate(movie.getReleaseDate())
                .rating(movie.getRating())
                .viewCount(movie.getViewCount())
                .status(movie.getStatus())
                .category(movie.getCategory() != null ? CategoryDTO.fromEntity(movie.getCategory()) : null)
                .build();
    }
}

