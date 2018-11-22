# Daily Press Review

[![CircleCI](https://circleci.com/gh/thierrymarianne/daily-press-review-clojure.svg?style=svg)](https://circleci.com/gh/thierrymarianne/daily-press-review-clojure)

Easing observation of Twitter lists to publish a daily press review

## Dependencies

Install Leiningen by following [the official instructions](https://github.com/technomancy/leiningen)

```
lein deps
```

## How to configure the project

```
cp lein-env{.dist,}
# fill the missing properties 
# in the newly created configuration file
```

## Available commands

### How to import first degree subscriptions?

```
# 1000 ; Maximum number of messages to be consumed
# 5    ; Parallel consumers
lein run consume-amqp-messages network [1000] [5]
```

### How to import favorited statuses?

```
# 100 ; Maximum number of messages to be consumed
# 2   ; Parallel consumers
lein run consume-amqp-messages likes [100] [2]
```

### How to import statuses from lists?

```
# 1 ; Maximum number of messages to be consumed
# 3 ; Parallel consumers
lein run consume-amqp-messages lists [1] [3]
```

### How to update bios of members?

```
lein run update-members-descriptions-urls
```

### How to recommend new subscriptions based on a history of subscriptions?

```
lein run recommend-subscriptions twitter_handle
```

## Tests

There is no 100% (very far from it) code coverage as intended behaviors heavily depend on Twitter API...  
OK, this is totally wrong (even though I didn't want to use actual tokens 
in continuous integration to test the API as data may vary, accounts might get suspended, protected or deleted).

However, previous implementations of mosts commands are available at 
[github.com/thierrymarianne/daily-press-review](https://github.com/thierrymarianne/daily-press-review)
and the commands outcomes being pretty much the same, I feel confident enough to carry changes regularly
without worring about breaking everything.

In order to worry less about breaking original intents (or breaking them wholeheartedly without worrying neither),
more tests will be added with new commands.

```
lein test
```
