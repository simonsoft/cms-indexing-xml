/**
 * Copyright (C) 2009-2017 Simonsoft Nordic AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.simonsoft.cms.indexing.xml;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** One item's live HEAD check, shared by XML extraction and its background batches. */
public final class XmlIndexingGuard {

    private final BooleanSupplier headCheck;
    private boolean current = true;

    /**
     * The caller supplies a live SVN check for this item and revision. It may run on
     * the writer thread, so capture resolved collaborators, not request-context proxies.
     * Calls are serialized; the callback must not share its SVN connection concurrently
     * with other readers outside this guard.
     */
    public XmlIndexingGuard(BooleanSupplier headCheck) {
        this.headCheck = Objects.requireNonNull(headCheck);
    }

    public synchronized boolean isCurrent() {
        if (current) {
            current = headCheck.getAsBoolean();
        }
        return current;
    }

    public void check() {
        if (!isCurrent()) {
            throw new StaleItemException();
        }
    }

    public synchronized boolean isStopped() {
        return !current;
    }

    public synchronized void stop() {
        current = false;
    }

    /** Obsolete XML work is skipped; lookup and write failures must still propagate. */
    public static final class StaleItemException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private StaleItemException() {
            super("XML item is no longer current at SVN HEAD");
        }
    }
}
