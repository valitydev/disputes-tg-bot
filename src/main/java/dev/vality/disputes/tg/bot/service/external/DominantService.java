package dev.vality.disputes.tg.bot.service.external;

import dev.vality.damsel.domain.Provider;
import dev.vality.damsel.domain.ProviderRef;
import dev.vality.damsel.domain.Terminal;
import dev.vality.damsel.domain.TerminalRef;
import dev.vality.damsel.domain_config.*;
import dev.vality.disputes.tg.bot.exception.DominantException;
import dev.vality.disputes.tg.bot.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DominantService {

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

    @Cacheable(value = "terminals", key = "#terminalRef.id", cacheManager = "terminalsCacheManager")
    public Terminal getTerminal(TerminalRef terminalRef) {
        return getTerminal(terminalRef, Reference.head(new Head()));
    }

    private Terminal getTerminal(TerminalRef terminalRef, Reference revisionReference) {
        log.debug("Trying to get terminal from dominant, terminalRef='{}', revisionReference='{}'", terminalRef,
                revisionReference);
        try {
            var reference = new dev.vality.damsel.domain.Reference();
            reference.setTerminal(terminalRef);
            var versionedObject = checkoutObject(revisionReference, reference);
            var terminal = versionedObject.getObject().getTerminal().getData();
            log.debug("Terminal has been found, terminalRef='{}', revisionReference='{}'",
                    terminalRef, revisionReference);
            return terminal;
        } catch (VersionNotFound | ObjectNotFound ex) {
            throw new NotFoundException(String.format("Version not found, terminalRef='%s', revisionReference='%s'",
                    terminalRef, revisionReference), ex);
        } catch (TException ex) {
            throw new DominantException(String.format("Failed to get terminal, terminalRef='%s'," +
                    " revisionReference='%s'", terminalRef, revisionReference), ex);
        }
    }

    private VersionedObject checkoutObject(Reference revisionReference, dev.vality.damsel.domain.Reference reference)
            throws TException {
        return dominantClient.checkoutObject(revisionReference, reference);
    }
}
