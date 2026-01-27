package com.example.demo.dto.movie;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<MovieDTO> banners;          // Phim nổi bật cho banner/slider
    private List<MovieDTO> newMovies;
    private List<MovieDTO> popularMovies;    // Phim phổ biến (theo view count)
    private List<MovieDTO> trendingMovies;   // Phim đang hot (theo rating)
    private List<CategoryDTO> categories;
}

