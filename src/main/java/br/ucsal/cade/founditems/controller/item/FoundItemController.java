package br.ucsal.cade.founditems.controller.item;

import br.ucsal.cade.founditems.controller.item.dto.FoundItemForm;
import br.ucsal.cade.founditems.controller.item.dto.FoundItemResponse;
import br.ucsal.cade.founditems.controller.item.dto.StorageBoxSuggestion;
import br.ucsal.cade.founditems.persistence.item.FoundItem;
import br.ucsal.cade.founditems.persistence.item.StorageBox;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    public ResponseEntity<FoundItemResponse> register(@Valid @ModelAttribute FoundItemForm form,
                                                      @AuthenticationPrincipal Jwt token) {
        RegisterFoundItem request = new RegisterFoundItem(bytesOf(form), form.categoryId(), form.block(),
                form.room(), form.foundOn(), form.storageBox(), form.risk(), form.internalDescription(),
                form.reservedDetails());
        FoundItem item = service.register(request, UUID.fromString(token.getSubject()));
        return ResponseEntity.created(URI.create("/found-items/" + item.getId())).body(FoundItemResponse.from(item));
    }

    @Override
    @GetMapping("/{id}")
    public FoundItemResponse find(@PathVariable UUID id) {
        return FoundItemResponse.from(service.find(id));
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
    public StorageBoxSuggestion suggestStorageBox(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return new StorageBoxSuggestion(StorageBox.suggestedFor(date));
    }

    private static byte[] bytesOf(FoundItemForm form) {
        try {
            return form.photo().getBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
