package com.hotelbooking.infrastructure.persistence;

import com.hotelbooking.domain.owner.OwnerAccount;
import com.hotelbooking.domain.owner.OwnerRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryOwnerRepository implements OwnerRepository {

    private final Map<String, OwnerAccount> store = new ConcurrentHashMap<>();

    @Override
    public OwnerAccount save(OwnerAccount owner) {
        Objects.requireNonNull(owner, "owner cannot be null");
        store.put(owner.getId(), owner);
        return owner;
    }

    @Override
    public Optional<OwnerAccount> findById(String ownerId) {
        if (ownerId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(ownerId.trim()));
    }

    @Override
    public List<OwnerAccount> findAll() {
        return List.copyOf(store.values());
    }
}
