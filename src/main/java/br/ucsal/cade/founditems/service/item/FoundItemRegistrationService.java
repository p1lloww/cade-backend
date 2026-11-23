package br.ucsal.cade.founditems.service.item;

import br.ucsal.cade.founditems.api.FoundItemData;
import br.ucsal.cade.founditems.api.FoundItemRegistration;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
class FoundItemRegistrationService implements FoundItemRegistration {

    private final FoundItemService items;

    FoundItemRegistrationService(FoundItemService items) {
        this.items = items;
    }

    @Override
    public UUID register(FoundItemData data, UUID staffId) {
        return items.register(new RegisterFoundItem(data.photo(), data.categoryId(), data.block(), data.room(),
                data.foundOn(), null, null, data.internalDescription(), data.reservedDetails()), staffId).getId();
    }
}
