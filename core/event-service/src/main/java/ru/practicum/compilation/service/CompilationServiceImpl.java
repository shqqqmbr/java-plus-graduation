package ru.practicum.compilation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationDto;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.NotFoundException;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final EventRepository eventRepository;
    private final CompilationRepository compilationRepository;

    private final CompilationMapper compilationMapper;

    // Admin API:
    @Override
    @Transactional
    public CompilationDto create(NewCompilationDto newDto) {
        List<Event> events = this.findEventsBy(newDto.getEvents());
        Compilation compilation = compilationMapper.toEntity(newDto);
        compilation.setEvents(events);
        compilation = compilationRepository.save(compilation);
        return compilationMapper.toDto(compilation);
    }

    @Override
    @Transactional
    public CompilationDto update(Long compId, UpdateCompilationDto updDto) {
        Compilation compilation = this.findCompilationBy(compId);
        compilation = compilationMapper.updateFromDto(updDto, compilation);

        if (updDto.getEvents() != null) {
            List<Event> events = eventRepository.findAllById(updDto.getEvents());
            compilation.setEvents(events);
        }
        compilation = compilationRepository.save(compilation);
        return compilationMapper.toDto(compilation);
    }

    @Override
    @Transactional
    public void delete(Long compId) {
        compilationRepository.deleteById(compId);
    }

    // Public API:
    @Override
    public List<CompilationDto> getAllBy(Boolean pinned, Integer from, Integer size) {
        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);
        List<Compilation> compilations = compilationRepository.findByPinned(pinned, pageable);

        return compilations.stream()
                .map(compilationMapper::toDto)
                .toList();
    }

    @Override
    public CompilationDto getBy(Long compId) {
        Compilation compilation = this.findCompilationBy(compId);
        return compilationMapper.toDto(compilation);
    }


    private Compilation findCompilationBy(Long compId) {
        return compilationRepository.findById(compId).orElseThrow(() -> new NotFoundException("Подборка не найдена"));
    }

    private List<Event> findEventsBy(Set<Long> eventsIds) {
        List<Event> events = eventRepository.findEventsByIdIn(eventsIds);

        if (events.size() != eventsIds.size()) {
            throw new NotFoundException("Некоторые события не найдены");
        }
        return events;
    }
}