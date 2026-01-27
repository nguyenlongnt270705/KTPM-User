package com.example.demo.service;

import com.example.demo.domain.Movie;
import com.example.demo.dto.movie.CategoryDTO;
import com.example.demo.dto.movie.HomeResponseDTO;
import com.example.demo.dto.movie.MovieDTO;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;
    private final CategoryRepository categoryRepository;

    private static final int BANNER_LIMIT = 5;
    private static final int MOVIE_LIST_LIMIT = 12;
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final double MIN_TRENDING_RATING = 4.0;

    public HomeResponseDTO getHomeData() {
        // Lấy tất cả phim active từ database
        List<Movie> allActiveMovies = movieRepository.findAll()
                .stream()
                .filter(movie -> ACTIVE_STATUS.equals(movie.getStatus()))
                .collect(Collectors.toList());

        // Lấy phim cho banner (top rated với poster)
        List<MovieDTO> banners = getBannerMovies(allActiveMovies);

        // Lấy phim mới nhất
        List<MovieDTO> newMovies = getNewMovies(allActiveMovies);

        // Lấy phim phổ biến (theo view count)
        List<MovieDTO> popularMovies = getPopularMovies(allActiveMovies);

        // Lấy phim trending (theo rating)
        List<MovieDTO> trendingMovies = getTrendingMovies(allActiveMovies);

        // Lấy danh sách thể loại
        List<CategoryDTO> categories = getCategories();

        return HomeResponseDTO.builder()
                .banners(banners)
                .newMovies(newMovies)
                .popularMovies(popularMovies)
                .trendingMovies(trendingMovies)
                .categories(categories)
                .build();
    }

    /**
     * Lấy danh sách phim cho banner
     * Điều kiện: có posterUrl, sắp xếp theo rating DESC, viewCount DESC
     * Giới hạn: BANNER_LIMIT phim
     * 
     * @param movies Danh sách phim active
     * @return List MovieDTO cho banner
     */
    private List<MovieDTO> getBannerMovies(List<Movie> movies) {
        return movies.stream()
                .filter(movie -> movie.getPosterUrl() != null && !movie.getPosterUrl().isEmpty())
                .sorted(Comparator
                        .comparing(Movie::getRating, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Movie::getViewCount, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(BANNER_LIMIT)
                .map(MovieDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách phim mới nhất
     * Sắp xếp theo createdDate DESC
     * Giới hạn: MOVIE_LIST_LIMIT phim
     * 
     * @param movies Danh sách phim active
     * @return List MovieDTO phim mới nhất
     */
    private List<MovieDTO> getNewMovies(List<Movie> movies) {
        return movies.stream()
                .sorted(Comparator
                        .comparing(Movie::getCreatedDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MOVIE_LIST_LIMIT)
                .map(MovieDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách phim phổ biến
     * Sắp xếp theo viewCount DESC
     * Giới hạn: MOVIE_LIST_LIMIT phim
     * 
     * @param movies Danh sách phim active
     * @return List MovieDTO phim phổ biến
     */
    private List<MovieDTO> getPopularMovies(List<Movie> movies) {
        return movies.stream()
                .sorted(Comparator
                        .comparing(Movie::getViewCount, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MOVIE_LIST_LIMIT)
                .map(MovieDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách phim trending
     * Điều kiện: rating >= MIN_TRENDING_RATING
     * Sắp xếp theo rating DESC, viewCount DESC
     * Giới hạn: MOVIE_LIST_LIMIT phim
     * 
     * @param movies Danh sách phim active
     * @return List MovieDTO phim trending
     */
    private List<MovieDTO> getTrendingMovies(List<Movie> movies) {
        return movies.stream()
                .filter(movie -> movie.getRating() != null && movie.getRating() >= MIN_TRENDING_RATING)
                .sorted(Comparator
                        .comparing(Movie::getRating, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Movie::getViewCount, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MOVIE_LIST_LIMIT)
                .map(MovieDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách thể loại đang active
     * Sắp xếp theo displayOrder ASC
     * 
     * @return List CategoryDTO
     */
    private List<CategoryDTO> getCategories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(CategoryDTO::fromEntity)
                .collect(Collectors.toList());
    }
}

