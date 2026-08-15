package ru.practicum.aggregator.collector.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.aggregator.model.Category;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryRequest;


@UtilityClass
public class CategoryMapper {

    public Category toEntity(NewCategoryRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        return category;
    }

    public Category toEntity(CategoryDto dto) {
        Category category = new Category();
        category.setId(dto.getId());
        category.setName(dto.getName());
        return category;
    }

    public CategoryDto toCategoryDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName()
        );
    }
}