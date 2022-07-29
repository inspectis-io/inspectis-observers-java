/*
 * Copyright © 2022 JINSPIRED B.V.
 */

package io.inspectis.observers.spi.alpha;

import io.humainary.observers.Observers;
import io.humainary.observers.Observers.Bootstrap;
import io.humainary.observers.Observers.Lens;
import io.humainary.observers.Observers.Operant;
import io.humainary.observers.spi.ObserversProvider;
import io.humainary.substrates.Substrates.*;

import java.util.function.Function;

/**
 * The SPI implementation of {@link ObserversProvider}.
 *
 * @author wlouth
 * @since 1.0
 */

@SuppressWarnings ( {"rawtypes", "unchecked"} )
final class Provider
  implements ObserversProvider {

  private static final Bootstrap BOOTSTRAP = ( closure, name ) -> null;
  private static final Lens      LENS      = ( closure, observable ) -> observable;
  private static final Operant   OPERANT   = ( closure, prev, value ) -> value;
  private static final Optic     OPTIC     = new Optic ( BOOTSTRAP, LENS, OPERANT );

  @Override
  public < C, O, V, R > Observers.Context< R > context (
    final Function< ? super Name, O > fn,
    final Observers.Optic< C, ? super O, ? super V, R > optic,
    final Environment environment
  ) {

    return
      new Context<> (
        environment,
        Spaces.function (
          fn,
          optic
        )
      );

  }


  @Override
  public < C, O, V, R > Observers.Context< R > context (
    final Lookup< O > lookup,
    final Observers.Optic< C, ? super O, ? super V, R > optic,
    final Environment environment
  ) {

    return
      new Context<> (
        environment,
        Spaces.lookup (
          lookup,
          optic
        )
      );

  }


  @Override
  public < C, O extends Component, V, R > Observers.Context< R > context (
    final Container< O > container,
    final Observers.Optic< C, ? super O, ? super V, R > optic,
    final Environment environment
  ) {

    return
      new Context<> (
        environment,
        Spaces.container (
          container,
          optic
        )
      );

  }


  @Override
  public < C, E, O, V, R > Observers.Context< R > context (
    final Source< E > source,
    final Function< ? super Event< E >, O > select,
    final Observers.Optic< C, ? super O, ? super V, R > optic,
    final Environment environment
  ) {

    return
      new Context<> (
        environment,
        Spaces.source (
          source,
          select,
          optic
        )
      );

  }


  @Override
  public < C, O, V, R > Observers.Optic< C, O, V, R > optic (
    final Bootstrap< C, ? extends R > bootstrap,
    final Lens< C, ? super O, ? extends V > lens,
    final Operant< C, ? super V, R > operant
  ) {

    return
      new Optic<> (
        bootstrap,
        lens,
        operant
      );

  }


  @Override
  public < C, O, V > Observers.Optic< C, O, V, V > optic (
    final Lens< C, ? super O, ? extends V > lens
  ) {

    return
      new Optic<> (
        bootstrap (),
        lens,
        operant ()
      );

  }


  @Override
  public < C, V, R > Observers.Optic< C, V, V, R > optic (
    final Operant< C, ? super V, R > operant
  ) {

    return
      new Optic<> (
        bootstrap (),
        lens (),
        operant
      );

  }


  @Override
  public < C, O > Lens< C, O, O > lens () {

    return
      LENS;

  }


  @Override
  public < C, V > Operant< C, V, V > operant () {

    return
      OPERANT;

  }


  @Override
  public < C, R > Bootstrap< C, R > bootstrap () {

    return
      BOOTSTRAP;

  }


  @Override
  public < O, C > Optic< C, O, O, O > optic () {

    return
      OPTIC;

  }

}
