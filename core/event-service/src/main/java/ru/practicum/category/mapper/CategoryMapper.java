package ru.practicum.category.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.CategoryRequestDto;
import ru.practicum.category.model.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryDto toDto(Category category);

    Category toEntity(CategoryDto dto);

    @Mapping(target = "id", ignore = true)
    Category toEntity(CategoryRequestDto dto);
}