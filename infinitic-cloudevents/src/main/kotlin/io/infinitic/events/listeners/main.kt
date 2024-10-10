import kotlinx.coroutines.channels.Channel

class Box<out M>(
    //  ...
)

interface Transport<S> {
  //  ...
}

interface Message {
  // ...
}

fun <T : Message> test(
  consumer: Transport<T>,
  channel: Channel<Box<T>> = Channel()
): Channel<Box<T>> {
  return channel
}

fun main() {
  lateinit var consumer: Transport<out Message>
  val c: Channel<Box<Message>> = Channel()
  test(consumer as Transport<Message>, c)

}
