# Bid Pitch

An implementation of the [Bid Pitch][pitch] card game. The goal is to
allow me and my family to play online while we're scattered across the
country.  Based on [Lockport, NY][lockport] local rules :)

Another goal of this project is to make use of interesting
technologies. The server is written in [Clojure][clj] and the client
is written in [ClojureScript][cljs]. Game logic is shared between them
by using [cljx][cljx].

Websockets handle most of the client-server communication.
[Http-kit][httpkit] provides good websocket implementation and James
Henderson's [Chord][chord] provides a nice [core.async][async]
wrapper. I'm not sure if it was necessary but it was very convenient.

Also on the front-end I experimented with Facebook's [React.js][react]
and David Nolen's [Om][om] for a UI rendered by pure functions. Also
not necessary, but I couldn't imagine going back to a life without them.

[pitch]: http://en.wikipedia.org/wiki/Pitch_(card_game)
[lockport]: http://en.wikipedia.org/wiki/Lockport_(city),_New_York
[clj]: http://clojure.org/
[cljs]: https://github.com/clojure/clojurescript
[cljx]: https://github.com/lynaghk/cljx
[httpkit]: http://http-kit.org/
[chord]: https://github.com/james-henderson/chord
[async]: https://github.com/clojure/core.async
[react]: http://facebook.github.io/react/
[om]: https://github.com/swannodette/om

## Prerequisites

You will need [Leiningen][1] 1.7.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein run

## TODO

## License

Copyright © 2014 FIXME
