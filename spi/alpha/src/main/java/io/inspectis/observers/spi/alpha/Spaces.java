/*
 * Copyright © 2022 JINSPIRED B.V.
 */

package io.inspectis.observers.spi.alpha;

import io.humainary.observers.Observers;
import io.humainary.observers.Observers.Bootstrap;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.humainary.substrates.Substrates.*;
import static java.lang.Thread.onSpinWait;

final class Spaces {

  private Spaces () {}


  // marker interface space objects
  @SuppressWarnings ( "MarkerInterface" )
  interface Subject {
  }


  interface Space< S extends Subject, R > {

    S subject (
      Name name
    );

    void sync (
      final Consumer< ? super Name > callback
    );

    default void release () {
    }

    R observe (
      Referent referent,
      S subject
    );

  }


  static final class StateSubject< C, R >
    implements Subject {

    private volatile Closure< C, R > closure;
    private volatile R               result;

    @SuppressWarnings ( "rawtypes" )
    private static final AtomicReferenceFieldUpdater< StateSubject, Closure > UPDATER =
      AtomicReferenceFieldUpdater.newUpdater (
        StateSubject.class,
        Closure.class,
        "closure"
      );


    static < C, R > StateSubject< C, R > of (
      final Name name,
      final Bootstrap< C, ? extends R > bootstrap
    ) {

      final var closure =
        new Closure< C, R > ();

      return
        new StateSubject<> (
          closure,
          bootstrap.initialize (
            closure,
            name
          )
        );

    }

    private StateSubject (
      final Closure< C, R > closure,
      final R initial
    ) {

      this.closure =
        closure;

      this.result =
        initial;

    }


    < O, V > R update (
      final O observable,
      final Observers.Optic< C, ? super O, V, R > optic
    ) {

      final var current =
        acquire ();

      try {

        //noinspection NonAtomicOperationOnVolatileField
        return
          result =
            current.calc (
              observable,
              result,
              optic
            );

      } finally {

        release (
          current
        );

      }

    }


    private void release (
      final Closure< C, R > current
    ) {

      closure =
        current;

    }


    private Closure< C, R > acquire () {

      Closure< C, R > current;

      while (
        ( current = closure ) == null ||
          !UPDATER.compareAndSet ( this, current, null )
      ) {

        // some other thread
        // is using the result
        onSpinWait ();

      }

      return
        current;

    }


    R get () {

      return
        result;

    }

  }


  static final class Closure< C, R >
    implements Observers.Closure< C > {

    private C state;

    < O, V > R calc (
      final O observable,
      final R last,
      final Observers.Optic< C, ? super O, V, R > optic
    ) {

      final var value =
        optic.capture (
          this,
          observable
        );

      return
        ( value != null )
        ? optic.compose ( this, last, value )
        : null;

    }


    @Override
    public void set (
      final C state
    ) {

      this.state =
        state;

    }


    @Override
    public C get () {

      return
        state;

    }

  }

  static final class ContainerSubject< C, O extends Referent, R >
    implements Subject {

    private final StateSubject< C, R > state;
    private final O                    target;

    ContainerSubject (
      final StateSubject< C, R > state,
      final O target
    ) {

      this.state =
        state;

      this.target =
        target;
    }

  }

  static final class ObservableSubject< C, O, R >
    implements Subject {

    private final StateSubject< C, R > state;
    private       O                    observable;

    ObservableSubject (
      final StateSubject< C, R > state
    ) {

      this.state =
        state;

    }


    void set (
      final O value
    ) {

      this.observable =
        value;

    }

  }

  static < C, O, V, R > Space< ObservableSubject< C, O, R >, R > function (
    final Function< ? super Name, ? extends O > fn,
    final Observers.Optic< C, ? super O, ? super V, R > optic
  ) {

    return
      new FnSpace<> (
        fn,
        optic
      );

  }


  static < C, O, V, R > Space< ObservableSubject< C, O, R >, R > lookup (
    final Lookup< O > lookup,
    final Observers.Optic< C, ? super O, ? super V, R > optic
  ) {

    return
      new LookupSpace<> (
        lookup,
        optic
      );

  }


  static < C, E, O, V, R > Space< StateSubject< C, R >, R > source (
    final Source< E > source,
    final Function< ? super Event< E >, O > select,
    final Observers.Optic< C, ? super O, ? super V, R > optic
  ) {

    return
      new SourceSpace<> (
        source,
        select,
        optic
      );

  }


  static < C, O extends Component, V, R > Space< ContainerSubject< C, O, R >, R > container (
    final Container< ? extends O > container,
    final Observers.Optic< C, ? super O, ? super V, R > optic
  ) {

    return
      new ContainerSpace<> (
        container,
        optic
      );

  }


  private abstract static class AbstractSpace< S extends Subject, C, O, V, R >
    implements Space< S, R > {

    final Map< Name, S >                                subjects = new ConcurrentHashMap<> ();
    final Observers.Optic< C, ? super O, ? super V, R > optic;

    AbstractSpace (
      final Observers.Optic< C, ? super O, ? super V, R > optic
    ) {

      this.optic =
        optic;

    }


    @Override
    public S subject (
      final Name name
    ) {

      return
        subjects.get (
          name
        );

    }

  }


  private static final class ContainerSpace< C, O extends Component, V, R >
    extends AbstractSpace< ContainerSubject< C, O, R >, C, O, V, R > {

    private final Container< ? extends O > container;

    private ContainerSpace (
      final Container< ? extends O > container,
      final Observers.Optic< C, ? super O, ? super V, R > optic
    ) {

      super (
        optic
      );

      this.container =
        container;

    }


    @Override
    public void sync (
      final Consumer< ? super Name > callback
    ) {

      // here we create check whether there
      // are any items in the container
      // that are not in the subject map

      final var map = subjects;

      container.forEach (
        observable -> {

          final var name =
            observable.
              reference ()
              .name ();

          final var subject =
            map.get (
              name
            );

          if ( subject == null ) {

            map.putIfAbsent (
              name,
              create (
                name,
                observable
              )
            );

            // notify sync callback
            // of the new subject
            callback.accept (
              name
            );

          }

        }
      );

    }


    @Override
    public ContainerSubject< C, O, R > subject (
      final Name name
    ) {

      final var subject =
        subjects.get (
          name
        );

      return
        subject != null
        ? subject
        : getAndAdd ( name );

    }


    private ContainerSubject< C, O, R > getAndAdd (
      final Name name
    ) {

      final var observable =
        container.get (
          name,
          null
        );

      return
        observable != null
        ? add ( name, observable )
        : null;

    }


    private ContainerSubject< C, O, R > add (
      final Name name,
      final O observable
    ) {

      return
        subjects.computeIfAbsent (
          name,
          key ->
            create (
              key,
              observable
            )
        );
    }


    private ContainerSubject< C, O, R > create (
      final Name name,
      final O observable
    ) {

      return
        new ContainerSubject<> (
          StateSubject.of (
            name,
            optic
          ),
          observable
        );

    }


    @Override
    public R observe (
      final Referent referent,
      final ContainerSubject< C, O, R > subject
    ) {

      return
        subject.state.update (
          subject.target,
          optic
        );

    }

  }


  private static final class SourceSpace< C, E, O, V, R >
    extends AbstractSpace< StateSubject< C, R >, C, O, V, R > {

    private final Subscription  subscription;
    private final Queue< Name > queue = new ConcurrentLinkedQueue<> ();

    SourceSpace (
      final Source< E > source,
      final Function< ? super Event< E >, O > select,
      final Observers.Optic< C, ? super O, ? super V, R > optic
    ) {

      super (
        optic
      );

      subscription =
        source.subscribe (
          ( reference, registrar ) -> {

            final var subject =
              create (
                reference
              );

            registrar
              .register (
                event ->
                  update (
                    subject,
                    select.apply (
                      event
                    )
                  )
              );

          }
        );

    }


    private void update (
      final StateSubject< C, R > subject,
      final O observable
    ) {

      subject.update (
        observable,
        optic
      );

    }


    private StateSubject< C, R > create (
      final Reference reference
    ) {

      final var name =
        reference.name ();

      final var subject =
        StateSubject.of (
          name,
          optic
        );

      subjects.put (
        name,
        subject
      );

      queue.add (
        name
      );

      return
        subject;

    }


    @Override
    public void sync (
      final Consumer< ? super Name > callback
    ) {

      Name name;
      while ( ( name = queue.poll () ) != null ) {

        callback.accept (
          name
        );

      }

    }


    @Override
    public void release () {

      super.release ();

      subscription.close ();

    }


    @Override
    public R observe (
      final Referent referent,
      final StateSubject< C, R > subject
    ) {

      return
        subject.get ();

    }

  }

  private abstract static class AbstractObservableSpace< C, O, V, R >
    extends AbstractSpace< ObservableSubject< C, O, R >, C, O, V, R > {

    AbstractObservableSpace (
      final Observers.Optic< C, ? super O, ? super V, R > optic
    ) {

      super (
        optic
      );

    }


    @Override
    public final ObservableSubject< C, O, R > subject (
      final Name name
    ) {

      final var subject =
        subjects.get (
          name
        );

      return
        subject != null
        ? subject
        : add ( name );

    }


    @Override
    public final void sync (
      final Consumer< ? super Name > callback
    ) {

      // we don't notify the consumer
      // because we only load on-demand
      // functions don't have iteration

      subjects.forEach (
        ( name, subject ) ->
          subject.set (
            resolve (
              name,
              subject.observable
            )
          )
      );

    }


    @Override
    public final R observe (
      final Referent referent,
      final ObservableSubject< C, O, R > subject
    ) {

      // if the subject value is not set we then
      // retrieve it on-demand and store it

      var observable =
        subject.observable;

      if ( observable == null ) {

        observable =
          resolve (
            referent
              .reference ()
              .name (),
            null
          );

        if ( observable != null ) {

          subject.set (
            observable
          );

        }

      }

      return
        observable != null
        ? subject.state.update ( observable, optic )
        : null;

    }


    protected abstract O resolve (
      final Name name,
      final O current
    );


    private ObservableSubject< C, O, R > add (
      final Name name
    ) {

      return
        subjects.computeIfAbsent (
          name,
          this::create
        );

    }


    private ObservableSubject< C, O, R > create (
      final Name key
    ) {

      return
        new ObservableSubject<> (
          StateSubject.of (
            key,
            optic
          )
        );

    }

  }


  private static final class FnSpace< C, O, V, R >
    extends AbstractObservableSpace< C, O, V, R > {

    private final Function< ? super Name, ? extends O > fn;

    FnSpace (
      final Function< ? super Name, ? extends O > fn,
      final Observers.Optic< C, ? super O, ? super V, R > optic
    ) {

      super (
        optic
      );

      this.fn =
        fn;

    }


    @Override
    protected O resolve (
      final Name name,
      final O observable
    ) {

      return
        fn.apply (
          name
        );

    }

  }

  private static final class LookupSpace< C, O, V, R >
    extends AbstractObservableSpace< C, O, V, R > {

    private final Lookup< O > lookup;

    private LookupSpace (
      final Lookup< O > lookup,
      final Observers.Optic< C, ? super O, ? super V, R > optic
    ) {

      super (
        optic
      );

      this.lookup =
        lookup;

    }


    @Override
    protected O resolve (
      final Name name,
      final O observable
    ) {

      return
        lookup.get (
          name,
          observable
        );

    }

  }

}
