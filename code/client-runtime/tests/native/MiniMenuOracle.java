import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.input.keyboard.KeyRecord;
import com.jagex.game.runetek5.client.input.keyboard.KeyRecorder;
import com.jagex.game.runetek5.client.input.mouse.AbstractMouseManager;
import com.jagex.game.runetek5.client.input.mouse.MouseRecord;
import com.jagex.game.runetek5.client.menu.ColoredMiniMenuEntry;
import com.jagex.game.runetek5.client.menu.MenuInteraction;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.client.menu.MiniMenuEntry;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.defaults.InteractionDefaultsType;
import com.jagex.game.runetek5.config.inputsubscriber.KeyHoldInputSubscriber;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.tools.Language;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Random;

public final class MiniMenuOracle {
  static final IdentityHashMap<Object, Integer> IDS = new IdentityHashMap<>();
  static int seq = 1;
  static final boolean[] FLAGS = new boolean[4];
  static final boolean[] KEYS = new boolean[4];
  static int mouseX;
  static int mouseY;
  static sun.misc.Unsafe unsafe;
  static Object me;
  static Object npc;
  static Class<?> pat;
  static Object[] players;
  static Object componentStub;
  static int screenW;
  static int screenH;
  static StringBuilder out = new StringBuilder();
  static boolean groupMode;
  static boolean curBold;
  static int curTop;
  static int cases;
  static int steps;
  static int packets;
  static int clickedSteps;
  static int fullCases;
  static int structureCases;
  static Random rnd;

  static void set(Class<?> c, String n, Object v) throws Exception {
    Field f = c.getDeclaredField(n);
    f.setAccessible(true);
    f.set(null, v);
  }

  static Object get(Class<?> c, String n) throws Exception {
    Field f = c.getDeclaredField(n);
    f.setAccessible(true);
    return f.get(null);
  }

  static void setField(Object o, Class<?> c, String n, Object v) throws Exception {
    Field f = c.getDeclaredField(n);
    f.setAccessible(true);
    f.set(o, v);
  }

  static String[] TAGS = {"", "<col=ffff00>Goblin", "<col=ffff00>Cow", "<col=ffff>Door", "<col=ffffff>Bob", "<col=ffffff>Alice the long named", "<col=ff0000>X", "<col=ffff00>Goblin (level: 5)"};
  static String[] ACTIONS = {"Attack", "Talk-to", "Open", "Take", "Walk here", "Examine", "Use", "Trade with", "Follow", "Cast spell on", "Use -> ", "Face here", "A", "Continue"};
  static long[] BITS = {0, 1, 17, 33, 2, 3, 100, 1L << 40, -5, 4};
  static int[] PLAYER_TYPES = {44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 2044, 2045, 2053, 15, 16};
  static int[] NPC_TYPES = {9, 10, 11, 12, 13, 1003, 8, 2009, 2010, 2011, 2012, 2013};
  static int[] LOC_TYPES = {3, 4, 5, 6, 1001, 1002, 2};
  static int[] OBJ_TYPES = {18, 19, 20, 21, 22, 1004, 17};
  static int[] TILE_TYPES = {60, 23, 59};
  static int[] PLAIN_TYPES = {0, 5, 1006, 1000};
  static int[] COMPONENT_TYPES = {57, 58, 1007, 25, 30};

  static int pick(int[] a) {
    return a[rnd.nextInt(a.length)];
  }

  static String pickS(String[] a) {
    return a[rnd.nextInt(a.length)];
  }

  static int idOf(Object o) {
    if (o == null) {
      return -1;
    }
    Integer i = IDS.get(o);
    return i == null ? -2 : i;
  }

  static void line(String s) {
    out.append(s).append('\n');
  }

  static void refreshWorld() throws Exception {
    players = (Object[]) get(Class.forName("com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate"), "players");
    for (int i = 0; i < 16; i++) {
      players[i] = me;
    }
    Object map = get(Client.class, "NPC_MAP");
    for (int i = 0; i < 16; i++) {
      ((com.jagex.game.runetek5.cache.node.IterableNodeMap) map).put(new com.jagex.game.runetek5.cache.node.impl.ObjectNode(npc), (long) i);
    }
    set(Client.class, "crossType", 9);
  }

  static void dump() throws Exception {
    StringBuilder s = new StringBuilder("o\tS");
    s.append('\t').append(MiniMenu.isOpen ? 1 : 0);
    s.append('\t').append(MiniMenu.isCollapsed ? 1 : 0);
    s.append('\t').append(MiniMenu.entryCount);
    s.append('\t').append(MiniMenu.coloredEntryCount);
    s.append('\t').append(idOf(MiniMenu.activeEntry));
    s.append('\t').append(idOf(MiniMenu.topEntry));
    s.append('\t').append(idOf(MiniMenu.secondaryEntry));
    s.append('\t').append(StaticMethods.menuOpenX);
    s.append('\t').append(StaticMethods.menuOpenY);
    s.append('\t').append(MiniMenu.menuWidth);
    s.append('\t').append(MiniMenu.menuHeight);
    s.append('\t').append(MiniMenu.submenuX);
    s.append('\t').append(MiniMenu.submenuY);
    s.append('\t').append(MiniMenu.submenuWidth);
    s.append('\t').append(com.jagex.game.runetek5.entity.gameobject.StationaryWall.submenuHeight);
    int sub = -1;
    int gi = 0;
    for (Object g = MiniMenu.sortedColoredEntries.getNext(); g != null; g = MiniMenu.sortedColoredEntries.getDirectNext()) {
      if (g == MiniMenu.activeSubmenu) {
        sub = gi;
      }
      gi++;
    }
    s.append('\t').append(sub);
    s.append('\t').append(MiniMenu.deferredClickType);
    s.append('\t').append(idOf(MiniMenu.deferredEntry));
    line(s.toString());
    StringBuilder l = new StringBuilder("o\tL");
    for (Object e = MiniMenu.entryList.getFirst(); e != null; e = MiniMenu.entryList.next()) {
      MiniMenuEntry m = (MiniMenuEntry) e;
      l.append('\t').append(idOf(m)).append(':').append(m.clickType);
    }
    line(l.toString());
    for (Object g = MiniMenu.sortedColoredEntries.getNext(); g != null; g = MiniMenu.sortedColoredEntries.getDirectNext()) {
      ColoredMiniMenuEntry c = (ColoredMiniMenuEntry) g;
      StringBuilder gl = new StringBuilder("o\tG\t").append(c.queueSize).append('\t').append(c.tagStart);
      for (Object e = c.nodeQueue.getNext(); e != null; e = c.nodeQueue.getDirectNext()) {
        gl.append('\t').append(idOf(e));
      }
      line(gl.toString());
    }
    StringBuilder p = new StringBuilder("o\tP");
    var q = Client.GAME_CONNECTION_CONTEXT.queuedPackets;
    Object o;
    while ((o = q.popHead()) != null) {
      TcpPacket t = (TcpPacket) o;
      p.append('\t').append(t.packet.id).append(':');
      byte[] b = t.buffer.buffer;
      for (int i = 1; i < t.capacity; i++) {
        p.append(String.format("%02x", b[i] & 0xff));
      }
      packets++;
    }
    line(p.toString());
  }

  static MiniMenuEntry newest() {
    return (MiniMenuEntry) MiniMenu.entryList.tail.previous;
  }

  static void add(boolean structure) throws Exception {
    String action = pickS(ACTIONS);
    String colored = pickS(TAGS);
    int family = rnd.nextInt(structure ? 12 : 10);
    int type;
    long uid;
    int p0 = rnd.nextInt(104);
    int p1 = rnd.nextInt(104);
    int item = -1;
    switch (family) {
      case 0:
      case 1:
        type = pick(PLAYER_TYPES);
        uid = rnd.nextInt(16);
        break;
      case 2:
      case 3:
        type = pick(NPC_TYPES);
        uid = rnd.nextInt(16);
        break;
      case 4:
      case 5:
        type = pick(LOC_TYPES);
        uid = ((long) rnd.nextInt(10) << 32) | ((long) rnd.nextInt(4) << 20) | rnd.nextInt(1 << 14);
        break;
      case 6:
        type = pick(OBJ_TYPES);
        uid = rnd.nextInt(10);
        if (rnd.nextInt(4) == 0) {
          item = rnd.nextInt(10);
        }
        break;
      case 7:
        type = pick(TILE_TYPES);
        uid = 0;
        break;
      case 8:
        type = pick(PLAIN_TYPES);
        uid = rnd.nextInt(16);
        break;
      case 9:
        type = pick(PLAYER_TYPES);
        uid = rnd.nextInt(16);
        break;
      default:
        type = pick(COMPONENT_TYPES);
        uid = rnd.nextInt(10);
        break;
    }
    if (type != 23 && type != 60 && type != 59 && family != 8 && family != 6 && rnd.nextInt(6) == 0) {
      item = -1;
    }
    long bits = rnd.nextInt(3) == 0 ? rnd.nextLong() : BITS[rnd.nextInt(BITS.length)];
    boolean sel = rnd.nextInt(6) != 0;
    boolean other = rnd.nextInt(7) == 0;
    boolean uncat = rnd.nextInt(7) == 0;
    if (groupMode) {
      colored = TAGS[1 + rnd.nextInt(2)];
      bits = BITS[1 + rnd.nextInt(3)];
      uncat = rnd.nextInt(10) == 0;
    }
    int cursor = rnd.nextInt(5) == 0 ? -1 : rnd.nextInt(30);
    line("A\t" + action + "\t" + colored + "\t" + cursor + "\t" + type + "\t" + item + "\t" + uid + "\t" + p0 + "\t" + p1 + "\t" + (sel ? 1 : 0) + "\t" + (other ? 1 : 0) + "\t" + bits + "\t" + (uncat ? 1 : 0));
    int before = MiniMenu.entryCount;
    MiniMenu.addEntry(action, colored, cursor, type, item, uid, p0, p1, sel, other, bits, uncat);
    if (MiniMenu.entryCount != before) {
      IDS.put(newest(), seq++);
    }
  }

  static int[] mouse() {
    if (!MiniMenu.isOpen) {
      return new int[] {rnd.nextInt(screenW), rnd.nextInt(screenH)};
    }
    int r = rnd.nextInt(10);
    int rowH = com.jagex.game.runetek5.client.menu.MiniMenu.entryRowHeight;
    int head = curBold ? curTop + 21 : 31;
    int adj = curBold ? 0 : -6;
    if (r < 4 && MiniMenu.activeSubmenu != null && rnd.nextBoolean()) {
      int row = rnd.nextInt(Math.max(1, MiniMenu.activeSubmenu.queueSize));
      return new int[] {MiniMenu.submenuX + rnd.nextInt(Math.max(1, MiniMenu.submenuWidth)), MiniMenu.submenuY + head + row * rowH + adj};
    }
    if (r < 5) {
      int rows = MiniMenu.isCollapsed ? MiniMenu.coloredEntryCount : MiniMenu.entryCount;
      int row = rnd.nextInt(Math.max(1, rows));
      return new int[] {StaticMethods.menuOpenX + rnd.nextInt(Math.max(1, MiniMenu.menuWidth)), StaticMethods.menuOpenY + head + row * rowH + adj};
    }
    if (r == 9) {
      return rnd.nextBoolean() ? new int[] {0, 0} : new int[] {screenW - 1, screenH - 1};
    }
    if (r >= 5 && MiniMenu.activeSubmenu != null) {
      int w = MiniMenu.submenuWidth;
      int h = com.jagex.game.runetek5.entity.gameobject.StationaryWall.submenuHeight;
      return new int[] {MiniMenu.submenuX - 14 + rnd.nextInt(w + 29), MiniMenu.submenuY - 14 + rnd.nextInt(h + 29)};
    }
    int w = MiniMenu.menuWidth;
    int h = MiniMenu.menuHeight;
    return new int[] {StaticMethods.menuOpenX - 14 + rnd.nextInt(w + 29), StaticMethods.menuOpenY - 14 + rnd.nextInt(h + 29)};
  }

  static void fire(boolean structure) throws Exception {
    boolean open = MiniMenu.isOpen;
    boolean primary = false;
    boolean secondary = false;
    boolean context = false;
    boolean openSub = false;
    if (open) {
      openSub = !structure && rnd.nextInt(5) < (MiniMenu.activeSubmenu != null ? 2 : 1);
    } else {
      int r = rnd.nextInt(20);
      if (structure) {
        context = true;
        primary = rnd.nextBoolean();
        secondary = rnd.nextInt(4) == 0;
      } else if (r < 8) {
        primary = true;
      } else if (r < 10) {
        secondary = true;
      } else if (r < 14) {
        context = true;
      } else if (r < 15) {
        primary = true;
        context = true;
      } else if (r < 16) {
        primary = true;
        secondary = true;
      } else if (r < 17) {
        secondary = true;
        context = true;
      }
    }
    boolean keyDown = rnd.nextInt(6) == 0;
    boolean run = rnd.nextInt(3) == 0;
    boolean drag = rnd.nextInt(25) == 0;
    int itemCount = rnd.nextInt(20) == 0 ? 1 : 0;
    int[] m = mouse();
    fireWith(primary, secondary, context, openSub, keyDown, run, drag, itemCount, m[0], m[1]);
  }

  static void fireWith(boolean primary, boolean secondary, boolean context, boolean openSub, boolean keyDown, boolean run, boolean drag, int itemCount, int mx, int my) throws Exception {
    line("F\t" + (primary ? 1 : 0) + "\t" + (secondary ? 1 : 0) + "\t" + (context ? 1 : 0) + "\t" + (openSub ? 1 : 0) + "\t" + (keyDown ? 1 : 0) + "\t" + (run ? 1 : 0) + "\t" + (drag ? 1 : 0) + "\t" + itemCount + "\t" + mx + "\t" + my);
    FLAGS[0] = primary;
    FLAGS[1] = secondary;
    FLAGS[2] = context;
    FLAGS[3] = openSub;
    KEYS[1] = keyDown;
    KEYS[2] = run;
    mouseX = mx;
    mouseY = my;
    set(Client.class, "draggingComponent", drag ? componentStub : null);
    set(Client.class, "menuItemCount", itemCount);
    refreshWorld();
    MenuInteraction.processMenuInput();
    steps++;
    dump();
  }

  static void reset() throws Exception {
    line("R");
    MiniMenu.resetEntries();
    dump();
  }

  static void runCase(int index) throws Exception {
    boolean structure = rnd.nextInt(5) == 0;
    boolean bold = rnd.nextInt(4) == 0;
    int maxVisible = rnd.nextInt(10) < 7 ? -1 : 3 + rnd.nextInt(10);
    int[] widths = {300, 765, 1024};
    int[] heights = {200, 360, 503, 800};
    screenW = widths[rnd.nextInt(widths.length)];
    screenH = heights[rnd.nextInt(heights.length)];
    int openMode = rnd.nextInt(4) == 0 ? 1 : 0;
    boolean always = rnd.nextInt(3) != 0;
    boolean face = rnd.nextBoolean();
    int[][] pads = {{12, 3}, {14, 4}, {10, 2}, {13, 5}};
    int[] pad = pads[rnd.nextInt(pads.length)];
    int cut = rnd.nextInt(30) == 0 ? 5 : -1;
    int selHash = rnd.nextInt(1 << 30);
    int selItem = rnd.nextInt(30000);
    int selSlot = rnd.nextInt(60);
    int playerIndex = rnd.nextInt(2000);
    int baseX = rnd.nextInt(3) == 0 ? 0 : 3000 + rnd.nextInt(400);
    int baseY = rnd.nextInt(3) == 0 ? 0 : 3000 + rnd.nextInt(400);
    byte[] fm = new byte[263];
    for (int i = 0; i < 256; i++) {
      fm[2 + i] = 6;
    }
    fm[261] = (byte) pad[0];
    fm[262] = (byte) pad[1];
    FontMetrics.defaultFontMetrics = new FontMetrics(fm);
    FontMetrics.useBoldFont = bold;
    curBold = bold;
    curTop = pad[0];
    MiniMenu.maxVisibleEntries = maxVisible;
    set(Class.forName("com.jagex.game.runetek5.client.GameShell"), "screenCanvasHeight", screenH * -1929118563);
    set(Class.forName("com.jagex.game.runetek5.client.GameShell"), "screenCanvasWidth", screenW);
    set(Client.class, "menuOpenMode", openMode);
    set(Client.class, "CURRENT_CUTSCENE", cut);
    Object ed = get(Client.class, "ENTITY_DEFAULTS");
    ed.getClass().getField("alwaysShowContextMenu").set(ed, always);
    set(Class.forName("com.jagex.game.runetek5.client.menu.MiniMenuBuilderKt"), "SHOW_FACE_HERE", face);
    set(Client.class, "selectedComponentHash", selHash);
    set(Client.class, "selectedItemId", selItem);
    set(Client.class, "selectedSlotId", selSlot);
    setField(me, pat, "playerIndex", playerIndex);
    Object region = com.jagex.game.runetek5.config.Resource.mapRegion;
    if (region != null) {
      Object base = region.getClass().getField("baseCoordinates").get(region);
      base.getClass().getField("x").set(base, baseX);
      base.getClass().getField("y").set(base, baseY);
    }
    line("C\t" + index + "\t" + (bold ? 1 : 0) + "\t" + maxVisible + "\t" + screenW + "\t" + screenH + "\t" + openMode + "\t" + (always ? 1 : 0) + "\t" + (face ? 1 : 0) + "\t" + pad[0] + "\t" + pad[1] + "\t" + cut + "\t" + selHash + "\t" + selItem + "\t" + selSlot + "\t" + playerIndex + "\t" + baseX + "\t" + baseY + "\t" + (structure ? 1 : 0));
    if (structure) {
      structureCases++;
    } else {
      fullCases++;
    }
    groupMode = rnd.nextInt(3) == 0;
    int rounds = 1 + rnd.nextInt(3);
    boolean big = index % 97 == 5;
    for (int round = 0; round < rounds; round++) {
      reset();
      int n = big ? 400 + rnd.nextInt(140) : (rnd.nextInt(6) == 0 ? rnd.nextInt(3) : rnd.nextInt(30));
      for (int i = 0; i < n; i++) {
        add(structure);
      }
      int count = 1 + rnd.nextInt(6);
      if (big) {
        count = 2;
      }
      for (int i = 0; i < count; i++) {
        fire(structure);
      }
      if (MiniMenu.isOpen) {
        fireWith(false, false, false, false, false, false, false, 0, screenW - 1, screenH - 1);
      }
    }
    line("E");
  }

  public static void main(String[] a) throws Exception {
    try {
      run(a);
    } catch (Throwable t) {
      t.printStackTrace();
      try (PrintStream ps = new PrintStream(a[0] + ".partial", "UTF-8")) {
        ps.print(out);
      }
      System.exit(1);
    }
  }

  static void run(String[] a) throws Exception {
    long seed = Long.parseLong(a[1]);
    int count = Integer.parseInt(a[2]);
    rnd = new Random(seed);
    Field uf = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
    uf.setAccessible(true);
    unsafe = (sun.misc.Unsafe) uf.get(null);
    var idt = (InteractionDefaultsType) unsafe.allocateInstance(InteractionDefaultsType.class);
    idt.primaryClickSubscriber = (m, k, n, r) -> FLAGS[0];
    idt.secondaryClickSubscriber = (m, k, n, r) -> FLAGS[1];
    idt.contextMenuSubscriber = (m, k, n, r) -> FLAGS[2];
    idt.openMinimenuInputSubscriber = (m, k, n, r) -> FLAGS[3];
    idt.minimenuKey = new KeyHoldInputSubscriber(new int[] {1});
    idt.toggleRunKey = new KeyHoldInputSubscriber(new int[] {2});
    idt.teleportKey = new KeyHoldInputSubscriber(new int[] {3});
    TypeLists.INTERACTION_DEFAULTS = idt;
    KeyRecorder.Companion.getClass();
    KeyRecorder.keyRecorder = new KeyRecorder() {
      public void prepareAwaitingKeys() {}
      public boolean held(int k) {
        return KEYS[k];
      }
      public KeyRecord popHead() {
        return null;
      }
      public void addKeyListeners() {}
    };
    MouseRecord.recorder = new AbstractMouseManager() {
      public boolean leftButtonDown() {
        return false;
      }
      public boolean rightButtonDown() {
        return false;
      }
      public boolean middleButtonDown() {
        return false;
      }
      public int getMouseX() {
        return mouseX;
      }
      public int getMouseY() {
        return mouseY;
      }
      public MouseRecord nextSubmission() {
        return null;
      }
      public void reset() {}
      public void syncMouseState() {}
    };
    Class<?> pe = Class.forName("com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity");
    me = unsafe.allocateInstance(pe);
    pat = Class.forName("com.jagex.game.runetek5.entity.pathingentity.PathingEntity");
    setField(me, pat, "movementBaseX", new int[] {-1, -1});
    setField(me, pat, "movementBaseY", new int[] {-1, -1});
    Class<?> pu = Class.forName("com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate");
    set(pu, "MY_PLAYER", me);
    players = (Object[]) get(pu, "players");
    set(Class.forName("com.jagex.game.runetek5.client.input.console.Console"), "displayedOutput", new String[500]);
    Class<?> ne = Class.forName("com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity");
    npc = unsafe.allocateInstance(ne);
    setField(npc, pat, "movementBaseX", new int[] {-1, -1});
    setField(npc, pat, "movementBaseY", new int[] {-1, -1});
    Class<?> nt = Class.forName("com.jagex.game.runetek5.config.npc.NPCType");
    ne.getField("definitions").set(npc, unsafe.allocateInstance(nt));
    Class<?> ed = Client.class.getDeclaredField("ENTITY_DEFAULTS").getType();
    set(Client.class, "ENTITY_DEFAULTS", unsafe.allocateInstance(ed));
    componentStub = unsafe.allocateInstance(Client.class.getDeclaredField("draggingComponent").getType());
    Client.CURRENT_LANGUAGE = Language.ENGLISH;
    Class<?> il = Class.forName("com.jagex.game.runetek5.config.item.ItemTypeList");
    Object itemList = unsafe.allocateInstance(il);
    Object typeCache = Class.forName("com.jagex.game.runetek5.cache.node.lru.LRUCache").getConstructor(int.class).newInstance(64);
    setField(itemList, il, "typeCache", typeCache);
    Class<?> itc = Class.forName("com.jagex.game.runetek5.config.item.ItemType");
    for (int i = 0; i < 10; i++) {
      Object it = unsafe.allocateInstance(itc);
      typeCache.getClass().getMethod("put", Object.class, long.class).invoke(typeCache, it, (long) i);
    }
    TypeLists.OBJ = (com.jagex.game.runetek5.config.item.ItemTypeList) itemList;
    Class<?> otl = Class.forName("com.jagex.game.runetek5.config.object.ObjectTypeList");
    Object locList = unsafe.allocateInstance(otl);
    Object locCache = Class.forName("com.jagex.game.runetek5.cache.node.lru.LRUCache").getConstructor(int.class).newInstance(256);
    setField(locList, otl, "cache", locCache);
    Class<?> otc = Class.forName("com.jagex.game.runetek5.config.object.ObjectType");
    for (int i = 0; i < 10; i++) {
      Object lt = unsafe.allocateInstance(otc);
      locCache.getClass().getMethod("put", Object.class, long.class).invoke(locCache, lt, (long) i);
    }
    Class<?> mrc = Class.forName("com.jagex.game.runetek5.scene.MapRegion");
    if (com.jagex.game.runetek5.config.Resource.mapRegion == null) {
      com.jagex.game.runetek5.config.Resource.mapRegion = (com.jagex.game.runetek5.scene.MapRegion) unsafe.allocateInstance(mrc);
      setField(com.jagex.game.runetek5.config.Resource.mapRegion, mrc, "baseCoordinates", new com.jagex.game.runetek5.movement.CoordGrid());
    }
    setField(com.jagex.game.runetek5.config.Resource.mapRegion, mrc, "loctypeList", locList);
    MiniMenu.cancelOption = new MiniMenuEntry("Cancel", "", 0, 1006, -1, 0L, 0, 0, true, false, 0L, true);
    IDS.put(MiniMenu.cancelOption, 0);
    for (int c = 0; c < count; c++) {
      runCase(c);
    }
    line("Z\t" + count);
    PrintStream ps = new PrintStream(a[0], "UTF-8");
    ps.print(out);
    ps.close();
    System.out.println("cases=" + count + " full=" + fullCases + " structure=" + structureCases + " steps=" + steps + " packets=" + packets);
    System.exit(0);
  }
}
