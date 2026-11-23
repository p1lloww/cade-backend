package br.ucsal.cade.lossreports.controller.report;

import br.ucsal.cade.lossreports.controller.report.dto.LossReportProofRequestDTO;
import br.ucsal.cade.lossreports.controller.report.dto.LossReportResponseDTO;
import br.ucsal.cade.lossreports.service.report.LossReportService;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.authorization.RequiresRole;
import br.ucsal.cade.shared.security.jwt.TokenClaims;
import br.ucsal.cade.shared.storage.StoredPhoto;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

@RestController
@RequestMapping("/loss-reports/{id}/proof")
class LossReportProofController implements LossReportProofApi {

    private final LossReportService service;

    LossReportProofController(LossReportService service) {
        this.service = service;
    }

    @Override
    @RequiresRole(Role.STUDENT)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LossReportResponseDTO attach(@PathVariable UUID id,
                                        @Valid @RequestPart(name = "proof", required = false)
                                        LossReportProofRequestDTO proof,
                                        @RequestPart(name = "file", required = false) MultipartFile file,
                                        @AuthenticationPrincipal Jwt token) {
        String serialNumber = proof == null ? null : proof.serialNumber();
        return LossReportResponseDTO.from(service.attachProof(id, UUID.fromString(token.getSubject()),
                bytesOf(file), serialNumber));
    }

    /** O dono vê a própria prova; o funcionário (e o administrador) vê a de qualquer registro, para conferir. */
    @Override
    @RequiresRole({Role.STUDENT, Role.STAFF})
    @GetMapping
    public ResponseEntity<byte[]> proof(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        boolean staff = Role.valueOf(token.getClaimAsString(TokenClaims.ROLE)).satisfies(Role.STAFF);
        StoredPhoto photo = service.proofOf(id, UUID.fromString(token.getSubject()), staff);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.type().mediaType()))
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(photo.content());
    }

    private static byte[] bytesOf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
