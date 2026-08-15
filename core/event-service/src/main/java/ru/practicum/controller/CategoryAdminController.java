package ru.practicum.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryRequest;
import ru.practicum.aggregator.service.CategoryService;

import static ru.practicum.constants.ApiConstants.CATEGORIES_BASE;
import static ru.practicum.constants.ApiConstants.CATEGORY_ID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(CATEGORIES_BASE)
@Validated
public class
CategoryAdminController {

    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody NewCategoryRequest request) {
        log.info("POST /admin/categories - создание категории: {}", request);
        return categoryService.createCategory(request);
    }

    @PatchMapping(CATEGORY_ID)
    public CategoryDto updateCategory(
            @Positive @PathVariable Long catId,
            @Valid @RequestBody CategoryDto categoryDto
    ) {
        log.info("PATCH /admin/categories/{} - обновление категории: {}", catId, categoryDto);
        return categoryService.updateCategory(catId, categoryDto);
    }

    @DeleteMapping(CATEGORY_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@Positive @PathVariable Long catId) {
        log.info("DELETE /admin/categories/{}", catId);
        categoryService.deleteCategory(catId);
    }
}