package com.hotelbooking.domain.owner;

import java.util.List;
import java.util.Optional;

public interface OwnerRepository {

    OwnerAccount save(OwnerAccount owner);

    Optional<OwnerAccount> findById(String ownerId);

    List<OwnerAccount> findAll();
}
