/*
 * Copyright © 2022 JINSPIRED B.V.
 */

package io.inspectis.observers.spi.alpha;

import io.humainary.observers.Observers;
import io.humainary.substrates.Substrates;
import io.humainary.substrates.Substrates.Inlet;
import io.humainary.substrates.sdk.AbstractInstrument;
import io.inspectis.observers.spi.alpha.Spaces.Space;
import io.inspectis.observers.spi.alpha.Spaces.Subject;

final class Observer< S extends Subject, R >
  extends AbstractInstrument< R >
  implements Observers.Observer< R > {

  private final Space< S, ? extends R > space;
  private       S                       subject;


  Observer (
    final Inlet< R > inlet,
    final Space< S, ? extends R > space
  ) {

    super (
      inlet
    );

    this.space =
      space;

  }


  @Override
  public void observe () {

    final S target =
      subject ();

    if ( target != null ) {

      final R result =
        space.observe (
          this,
          target
        );

      if ( result != null ) {

        inlet.emit (
          result
        );

      }

    }

  }


  @Override
  public < C, V > V observe (
    final Observers.Lens< C, ? super R, V > lens,
    final Observers.Closure< C > closure
  ) {

    final S target =
      subject ();

    if ( target != null ) {

      final R result =
        space.observe (
          this,
          target
        );

      if ( result != null ) {

        inlet.emit (
          result
        );

        return
          lens.capture (
            closure,
            result
          );

      }

    }

    return
      null;

  }

  private S subject () {

    var result =
      this.subject;

    if ( result == null ) {

      subject
        = result
        = space.subject ( name () );

    }

    return
      result;

  }

  private Substrates.Name name () {

    return
      inlet
        .reference ()
        .name ();

  }

}
