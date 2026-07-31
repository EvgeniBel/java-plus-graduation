package ru.practicum.service;



import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.CreateCompilationDto;
import ru.practicum.dto.compilation.GetManyCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationDto;

import java.util.List;

public interface CompilationService {
    CompilationDto createCompilation(CreateCompilationDto dto);

    CompilationDto updateCompilation(UpdateCompilationDto dto);

    void removeCompilation(Long compId);

    List<CompilationDto> getCompilations(GetManyCompilationDto dto);

    CompilationDto getCompilationById(Long compId);
}
