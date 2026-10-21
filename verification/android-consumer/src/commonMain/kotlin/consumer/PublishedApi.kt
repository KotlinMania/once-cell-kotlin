package consumer

import io.github.kotlinmania.oncecell.race.OnceBool
import io.github.kotlinmania.oncecell.race.OnceBox
import io.github.kotlinmania.oncecell.race.OnceNonZeroUsize
import io.github.kotlinmania.oncecell.sync.IntOnceCell as SyncIntOnceCell
import io.github.kotlinmania.oncecell.sync.Lazy as SyncLazy
import io.github.kotlinmania.oncecell.sync.OnceCell as SyncOnceCell
import io.github.kotlinmania.oncecell.sync.StringOnceCell as SyncStringOnceCell
import io.github.kotlinmania.oncecell.unsync.IntOnceCell as UnsyncIntOnceCell
import io.github.kotlinmania.oncecell.unsync.Lazy as UnsyncLazy
import io.github.kotlinmania.oncecell.unsync.OnceCell as UnsyncOnceCell
import io.github.kotlinmania.oncecell.unsync.StringOnceCell as UnsyncStringOnceCell

public class ConsumerSyncCell<T : Any>(
    initial: T? = null,
) : SyncOnceCell<T>(initial)

public class ConsumerUnsyncCell<T : Any>(
    initial: T? = null,
) : UnsyncOnceCell<T>(initial)

public fun newSyncStringCell(): SyncStringOnceCell = SyncStringOnceCell.new()

public fun newSyncIntCell(): SyncIntOnceCell = SyncIntOnceCell.new()

public fun newUnsyncStringCell(): UnsyncStringOnceCell = UnsyncStringOnceCell.new()

public fun newUnsyncIntCell(): UnsyncIntOnceCell = UnsyncIntOnceCell.new()

public fun <T : Any> newSyncLazy(init: () -> T): SyncLazy<T> = SyncLazy.new { init() }

public fun <T : Any> newUnsyncLazy(init: () -> T): UnsyncLazy<T> = UnsyncLazy.new { init() }

public fun newRaceBool(): OnceBool = OnceBool.new()

public fun newRaceUsize(): OnceNonZeroUsize = OnceNonZeroUsize.new()

public fun <T : Any> newRaceBox(): OnceBox<T> = OnceBox.new()
