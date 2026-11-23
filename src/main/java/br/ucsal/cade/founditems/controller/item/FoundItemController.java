package br.ucsal.cade.founditems.controller.item;

import br.ucsal.cade.founditems.controller.item.dto.FoundItemBroughtByRequestDTO;
import br.ucsal.cade.founditems.controller.item.dto.FoundItemIdentificationRequestDTO;
import br.ucsal.cade.founditems.controller.item.dto.FoundItemRequestDTO;
import br.ucsal.cade.founditems.controller.item.dto.FoundItemResponseDTO;
import br.ucsal.cade.founditems.controller.item.dto.FoundItemUpdateRequestDTO;
import br.ucsal.cade.founditems.controller.item.dto.StorageBoxSuggestionResponseDTO;
import br.ucsal.cade.founditems.persistence.item.FoundItem;
import br.ucsal.cade.founditems.persistence.item.StorageBox;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.IdentifyFoundItem;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
import br.ucsal.cade.founditems.service.item.UpdateFoundItem;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.authorization.RequiresRole;
import br.ucsal.cade.shared.storage.StoredPhoto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/found-items")
@RequiresRole(Role.STAFF)
class FoundItemController implements FoundItemApi {

    private final FoundItemService service;

    FoundItemController(FoundItemService service) {
        this.service = service;
    }

    @Override
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FoundItemResponseDTO> register(@Valid @ModelAttribute FoundItemRequestDTO form,
                                                      @AuthenticationPrincipal Jwt token) {
        RegisterFoundItem request = new RegisterFoundItem(bytesOf(form), form.categoryId(), form.block(),
                form.room(), form.foundOn(), form.storageBox(), form.risk(), form.internalDescription(),
                form.reservedDetails());
        FoundItem item = service.register(request, form.broughtBy(), UUID.fromString(token.getSubject()));
        return ResponseEntity.created(URI.create("/found-items/" + item.getId())).body(FoundItemResponseDTO.from(item));
    }

    @Override
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public FoundItemResponseDTO update(@PathVariable UUID id, @Valid @RequestBody FoundItemUpdateRequestDTO request,
                                       @AuthenticationPrincipal Jwt token) {
        UpdateFoundItem command = new UpdateFoundItem(request.categoryId(), request.block(), request.room(),
                request.foundOn(), request.storageBox(), request.risk(), request.internalDescription(),
                request.reservedDetails(), request.reason(), request.version());
        return FoundItemResponseDTO.from(service.update(id, command, UUID.fromString(token.getSubject())));
    }

    @Override
    @PutMapping(path = "/{id}/brought-by", consumes = MediaType.APPLICATION_JSON_VALUE)
    public FoundItemResponseDTO recordBroughtBy(@PathVariable UUID id,
                                                @Valid @RequestBody FoundItemBroughtByRequestDTO request,
                                                @AuthenticationPrincipal Jwt token) {
        return FoundItemResponseDTO.from(service.recordBroughtBy(id, request.broughtBy(),
                UUID.fromString(token.getSubject())));
    }

    @Override
    @PutMapping(path = "/{id}/identification", consumes = MediaType.APPLICATION_JSON_VALUE)
    public FoundItemResponseDTO identify(@PathVariable UUID id,
                                         @Valid @RequestBody FoundItemIdentificationRequestDTO request,
                                         @AuthenticationPrincipal Jwt token) {
        return FoundItemResponseDTO.from(service.identify(id,
                new IdentifyFoundItem(request.name(), request.email(), request.document()),
                UUID.fromString(token.getSubject())));
    }

    @Override
    @DeleteMapping("/{id}/identification")
    public FoundItemResponseDTO clearIdentification(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
        return FoundItemResponseDTO.from(service.clearIdentification(id, UUID.fromString(token.getSubject())));
    }

    @Override
    @GetMapping("/{id}")
    public FoundItemResponseDTO find(@PathVariable UUID id) {
        return FoundItemResponseDTO.from(service.find(id));
    }

    @Override
    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> photo(@PathVariable UUID id) {
        StoredPhoto photo = service.photoOf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.type().mediaType()))
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(photo.content());
    }

    @Override
    @GetMapping("/storage-box-suggestion")
    public StorageBoxSuggestionResponseDTO suggestStorageBox(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return new StorageBoxSuggestionResponseDTO(StorageBox.suggestedFor(date));
    }

    private static byte[] bytesOf(FoundItemRequestDTO form) {
        try {
            return form.photo().getBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
