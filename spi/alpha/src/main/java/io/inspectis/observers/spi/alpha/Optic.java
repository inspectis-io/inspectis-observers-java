/*
 * Copyright © 2022 JINSPIRED B.V.
 */

package io.inspectis.observers.spi.alpha;

import io.humainary.observers.Observers;
import io.humainary.observers.Observers.Bootstrap;
import io.humainary.observers.Observers.Closure;
import io.humainary.observers.Observers.Lens;
import io.humainary.observers.Observers.Operant;
import io.humainary.substrates.Substrates.Name;

final class Optic< C, O, V, R >
  implements Observers.Optic< C, O, V, R > {

  private final Bootstrap< C, ? extends R >       bootstrap;
  private final Lens< C, ? super O, ? extends V > lens;
  private final Operant< C, ? super V, R >        operant;

  Optic (
    final Bootstrap< C, ? extends R > boostrap,
    final Lens< C, ? super O, ? extends V > lens,
    final Operant< C, ? super V, R > operant
  ) {

    this.bootstrap =
      boostrap;

    this.lens =
      lens;

    this.operant =
      operant;

  }


  @Override
  public R initialize (
    final Closure< C > closure,
    final Name name
  ) {

    return
      bootstrap.initialize (
        closure,
        name
      );

  }


  @Override
  public V capture (
    final Closure< C > closure,
    final O observable
  ) {

    return
      lens.capture (
        closure,
        observable
      );

  }


  @Override
  public R compose (
    final Closure< C > closure,
    final R prev,
    final V value
  ) {

    return
      operant.compose (
        closure,
        prev,
        value
      );

  }


}
