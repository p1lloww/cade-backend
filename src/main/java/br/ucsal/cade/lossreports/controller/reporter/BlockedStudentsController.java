package br.ucsal.cade.lossreports.controller.reporter;

import br.ucsal.cade.lossreports.controller.reporter.dto.BlockedStudentResponseDTO;
import br.ucsal.cade.lossreports.controller.reporter.dto.StudentReleaseRequestDTO;
import br.ucsal.cade.lossreports.service.reporter.StudentReleaseService;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.authorization.RequiresRole;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/blocked-students")
@RequiresRole(Role.ADMIN)
class BlockedStudentsController implements BlockedStudentsApi {

    private final StudentReleaseService service;

    BlockedStudentsController(StudentReleaseService service) {
        this.service = service;
    }

    @Override
    @GetMapping
    public List<BlockedStudentResponseDTO> blocked() {
        return service.blocked().stream().map(BlockedStudentResponseDTO::from).toList();
    }

    @Override
    @PostMapping(path = "/{studentId}/release", consumes = MediaType.APPLICATION_JSON_VALUE)
    public BlockedStudentResponseDTO release(@PathVariable UUID studentId,
                                             @Valid @RequestBody StudentReleaseRequestDTO request,
                                             @AuthenticationPrincipal Jwt token) {
        return BlockedStudentResponseDTO.from(service.release(studentId, request.reason().strip(),
                UUID.fromString(token.getSubject())));
    }
}
