package dev.vality.disputes.tg.bot.core.service;

import dev.vality.damsel.domain.Provider;
import dev.vality.damsel.domain.ProviderRef;
import dev.vality.damsel.domain_config.*;
import dev.vality.disputes.tg.bot.core.exception.DominantException;
import dev.vality.disputes.tg.bot.core.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DominantCacheServiceImpl {

    private final RepositoryClientSrv.Iface dominantClient;

    @Cacheable(value = "providers", key = "#providerRef.id", cacheManager = "providersCacheManager")
    public Provider getProvider(ProviderRef providerRef) {
        return getProvider(providerRef, Reference.head(new Head()));
    }

    private Provider getProvider(ProviderRef providerRef, Reference revisionReference) {
        log.debug("Trying to get provider from dominant, providerRef='{}', revisionReference='{}'", providerRef,
                revisionReference);
        try {
            var reference = new dev.vality.damsel.domain.Reference();
            reference.setProvider(providerRef);
            var versionedObject = checkoutObject(revisionReference, reference);
            var provider = versionedObject.getObject().getProvider().getData();
            log.debug("Provider has been found, providerRef='{}', revisionReference='{}'",
                    providerRef, revisionReference);
            return provider;
        } catch (VersionNotFound | ObjectNotFound ex) {
            throw new NotFoundException(String.format("Version not found, providerRef='%s', revisionReference='%s'",
                    providerRef, revisionReference), ex);
        } catch (TException ex) {
            throw new DominantException(String.format("Failed to get provider, providerRef='%s'," +
                    " revisionReference='%s'", providerRef, revisionReference), ex);
        }
    }

    private VersionedObject checkoutObject(Reference revisionReference, dev.vality.damsel.domain.Reference reference)
            throws TException {
        return dominantClient.checkoutObject(revisionReference, reference);
    }
}
