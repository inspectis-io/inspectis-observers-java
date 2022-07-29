/*
 * Copyright © 2022 JINSPIRED B.V.
 */

package io.inspectis.observers.spi.alpha;

import io.humainary.observers.spi.ObserversProvider;
import io.humainary.spi.Providers.Factory;

/**
 * The SPI provider factory implementation of {@link ObserversProvider}.
 *
 * @author wlouth
 * @since 1.0
 */

public final class ProviderFactory
  implements Factory< ObserversProvider > {

  @Override
  public ObserversProvider create () {

    return
      new Provider ();

  }

}
