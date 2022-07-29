/*
 * Copyright © 2022 JINSPIRED B.V.
 */

package io.inspectis.observers.spi.alpha;

import io.humainary.observers.Observers;
import io.humainary.substrates.Substrates.Environment;
import io.humainary.substrates.Substrates.Name;
import io.humainary.substrates.Substrates.Type;
import io.humainary.substrates.sdk.AbstractContext;
import io.inspectis.observers.spi.alpha.Spaces.Space;

import static io.humainary.observers.Observers.Observer.TYPE;

final class Context< S extends Spaces.Subject, R >
  extends AbstractContext< Observers.Observer< R >, R >
  implements Observers.Context< R > {

  private final Space< S, R > space;

  Context (
    final Environment environment,
    final Space< S, R > space
  ) {

    super (
      environment,
      inlet ->
        new Observer<> (
          inlet,
          space
        )
    );

    this.space =
      space;

  }


  @Override
  public void close () {

    space.
      release ();

  }


  @Override
  public Observers.Observer< R > observer (
    final Name name
  ) {

    return
      instrument (
        name
      );

  }


  @Override
  public void sync () {

    space.sync (
      this::instrument
    );

  }

  @Override
  protected Type type () {

    return
      TYPE;

  }

}
