package gtu.graduation.project.cryptoradar.model;

import java.util.List;

public record LogFilter(Long start, Long end, List<String> addresses) {
}