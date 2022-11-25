package net.messer.mystical_index.item.custom.page.type;

import com.google.common.collect.ImmutableList;
import net.messer.mystical_index.MysticalIndex;
import net.messer.mystical_index.block.entity.MysticalLecternBlockEntity;
import net.messer.mystical_index.client.tooltip.ItemStorageTooltipData;
import net.messer.mystical_index.item.custom.page.AttributePageItem;
import net.messer.mystical_index.item.custom.page.TypePageItem;
import net.messer.mystical_index.util.BigStack;
import net.messer.mystical_index.util.ContentsIndex;
import net.messer.mystical_index.util.request.ExtractionRequest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.item.TooltipData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static net.messer.mystical_index.item.ModItems.*;

public class ItemStorageTypePage extends TypePageItem {
    public static final String MAX_STACKS_TAG = "max_stacks";
    public static final String MAX_TYPES_TAG = "max_types";

    public ItemStorageTypePage(String id) {
        super(id);
    }

    @Override
    public int getColor() {
        return 0x88ff88;
    }

    @Override
    public MutableText getTypeDisplayName() {
        return super.getTypeDisplayName().formatted(Formatting.DARK_AQUA);
    }

    public static final String OCCUPIED_STACKS_TAG = "occupied_stacks";
    public static final String OCCUPIED_TYPES_TAG = "occupied_types";

    public static final String FILTERS_TAG = "filters";
    public static final String ITEM_FILTERS_TAG = "item";
    public static final String TAG_FILTERS_TAG = "tag";

    @Override
    public void onCraftToBook(ItemStack page, ItemStack book) {
        super.onCraftToBook(page, book);

        NbtCompound attributes = getAttributes(book);

        attributes.putInt(MAX_STACKS_TAG, 1);
        attributes.putInt(MAX_TYPES_TAG, 2);
    }

    public int getMaxTypes(ItemStack book) {
        return getAttributes(book).getInt(MAX_TYPES_TAG);
    }

    public int getMaxStack(ItemStack book) {
        return getAttributes(book).getInt(MAX_STACKS_TAG);
    }

    public ContentsIndex getContents(ItemStack book) {
        NbtCompound nbtCompound = book.getNbt();
        ContentsIndex result = new ContentsIndex();
        if (nbtCompound != null) {
            NbtList nbtList = nbtCompound.getList("Items", 10);
            Stream<NbtElement> nbtStream = nbtList.stream();
            Objects.requireNonNull(NbtCompound.class);
            nbtStream.map(NbtCompound.class::cast).forEach(
                    nbt -> result.add(ItemStack.fromNbt(nbt.getCompound("Item")), nbt.getInt("Count")));
        }
        return result;
    }

    protected int getFullness(ItemStack book) {
        int result = 0;
        for (BigStack bigStack : getContents(book).getAll()) {
            result += bigStack.getAmount() * getItemOccupancy(bigStack.getItem());
        }
        return result;
    }

    private int getItemOccupancy(Item item) {
        if (item.getMaxCount() == 1) {
            return 8;
        }
        return 1;
    }

    private Optional<NbtCompound> canMergeStack(ItemStack stack, NbtList items) {
        return items.stream()
                .filter(NbtCompound.class::isInstance)
                .map(NbtCompound.class::cast)
                .filter(item -> ItemStack.canCombine(ItemStack.fromNbt(item.getCompound("Item")), stack))
                .findFirst();
    }

    public boolean isFiltered(ItemStack book) {
        return !book.getOrCreateSubNbt(FILTERS_TAG).getList(ITEM_FILTERS_TAG, NbtElement.STRING_TYPE).isEmpty();
    }

    public boolean isFilteredTo(ItemStack book, ItemStack stack) {
        NbtCompound filters = book.getOrCreateNbt().getCompound(FILTERS_TAG);

        return filters.getList(ITEM_FILTERS_TAG, NbtElement.STRING_TYPE)
                .contains(NbtString.of(Registries.ITEM.getId(stack.getItem()).toString()));
    }

    public List<Item> getFilteredItems(ItemStack book) {
        NbtCompound filters = book.getOrCreateNbt().getCompound(FILTERS_TAG);
        return filters.getList(ITEM_FILTERS_TAG, NbtElement.STRING_TYPE).stream()
                .map(NbtString.class::cast)
                .map(NbtString::asString)
                .map(Identifier::tryParse)
                .filter(Objects::nonNull)
                .map(Registries.ITEM::get)
                .collect(ImmutableList.toImmutableList());
    }

    public void addFilteredItem(ItemStack book, Item item) {
        NbtCompound filters = book.getOrCreateSubNbt(FILTERS_TAG);
        NbtList itemFilters = filters.getList(ITEM_FILTERS_TAG, NbtElement.STRING_TYPE);
        itemFilters.add(NbtString.of(Registries.ITEM.getId(item).toString()));
        filters.put(ITEM_FILTERS_TAG, itemFilters);
    }

    public void removeFilteredItem(ItemStack book, int i) {
        NbtCompound filters = book.getOrCreateSubNbt(FILTERS_TAG);
        filters.getList(ITEM_FILTERS_TAG, NbtElement.STRING_TYPE).remove(i);
    }

    protected boolean canInsert(ItemStack book, ItemStack itemStack) {
        return itemStack.getItem().canBeNested();
    }

    private boolean canInsertFiltered(ItemStack book, ItemStack itemStack) {
        if (!canInsert(book, itemStack)) return false;

        if (!isFiltered(book)) return true;
        return isFilteredTo(book, itemStack);
    }

    protected int getBaseInsertPriority(ItemStack book) {
        return 0;
    }

    public int getInsertPriority(ItemStack book, ItemStack stack) {
        if (!canInsertFiltered(book, stack)) return -1;
        var base = getBaseInsertPriority(book);
        if (isFilteredTo(book, stack)) return base + 100;
        return base;
    }

    public int tryAddItem(ItemStack book, ItemStack stack) {
        if (stack.isEmpty() || !canInsertFiltered(book, stack)) {
            return 0;
        }
        NbtCompound bookNbt = book.getOrCreateNbt();
        if (!bookNbt.contains("Items")) {
            bookNbt.put("Items", new NbtList());
        }

        int maxFullness = getMaxStack(book) * 64;
        int fullnessLeft = maxFullness - getFullness(book);
        int canBeTakenAmount = Math.min(stack.getCount(), fullnessLeft / getItemOccupancy(stack.getItem()));
        if (canBeTakenAmount == 0) {
            return 0;
        }

        NbtList itemsList = bookNbt.getList("Items", 10);
        Optional<NbtCompound> mergeAbleStack = canMergeStack(stack, itemsList);
        if (mergeAbleStack.isPresent()) {
            NbtCompound mergeStack = mergeAbleStack.get();
            mergeStack.putInt("Count", mergeStack.getInt("Count") + canBeTakenAmount);
            itemsList.remove(mergeStack);
            itemsList.add(0, mergeStack);
        } else {
            if (itemsList.size() >= getMaxTypes(book)) {
                return 0;
            }

            ItemStack insertStack = stack.copy();
            insertStack.setCount(1);
            NbtCompound insertNbt = new NbtCompound();
            insertNbt.put("Item", insertStack.writeNbt(new NbtCompound()));
            insertNbt.putInt("Count", canBeTakenAmount);
            itemsList.add(0, insertNbt);
        }

        saveOccupancy(bookNbt,
                maxFullness - fullnessLeft + canBeTakenAmount * getItemOccupancy(stack.getItem()),
                itemsList.size());

        return canBeTakenAmount;
    }

    public Optional<ItemStack> removeFirstStack(ItemStack book) {
        return removeFirstStack(book, null);
    }

    public Optional<ItemStack> removeFirstStack(ItemStack book, Integer maxAmount) {
        NbtCompound bookNbt = book.getOrCreateNbt();
        if (!bookNbt.contains("Items")) {
            return Optional.empty();
        }
        NbtList itemsList = bookNbt.getList("Items", 10);
        if (itemsList.isEmpty()) {
            return Optional.empty();
        }
        NbtCompound firstItem = itemsList.getCompound(0);
        ItemStack itemStack = ItemStack.fromNbt(firstItem.getCompound("Item"));
        int itemCount = firstItem.getInt("Count");
        int takeCount = Math.min(itemCount, itemStack.getMaxCount());
        if (maxAmount != null) {
            takeCount = Math.min(takeCount, maxAmount);
        }

        itemStack.setCount(takeCount);

        if (takeCount >= itemCount) {
            itemsList.remove(0);
            if (itemsList.isEmpty()) {
                book.removeSubNbt("Items");
            }
        } else {
            firstItem.putInt("Count", itemCount - takeCount);
        }

        saveOccupancy(bookNbt, getFullness(book), itemsList.size());

        return Optional.of(itemStack);
    }

    public Optional<ItemStack> tryRemoveFirstStack(ItemStack book, int amount, Predicate<ItemStack> condition) {
        var stack = removeFirstStack(book, amount);
        if (stack.isPresent() && !condition.test(stack.get())) {
            tryAddItem(book, stack.get());
            return Optional.empty();
        }
        return stack;
    }

    public List<ItemStack> extractItems(ItemStack book, ExtractionRequest request, boolean apply) {
        if (request.isSatisfied())
            return Collections.emptyList();

        NbtCompound bookNbt = book.getOrCreateNbt();
        if (!bookNbt.contains("Items"))
            return Collections.emptyList();

        NbtList itemsList = bookNbt.getList("Items", 10);
        if (itemsList.isEmpty())
            return Collections.emptyList();

        ImmutableList.Builder<ItemStack> builder = ImmutableList.builder();

        for (int i = 0; i < itemsList.size(); i++) {
            NbtCompound nbtItem = itemsList.getCompound(i);
            ItemStack itemStack = ItemStack.fromNbt(nbtItem.getCompound("Item"));

            if (request.matches(itemStack.getItem())) {
                int itemCount = nbtItem.getInt("Count");
                int extractAmount = Math.min(itemCount, request.getAmountUnsatisfied());
                int stackSize = itemStack.getItem().getMaxCount();

                request.satisfy(extractAmount);
                if (apply) {
                    if (extractAmount >= itemCount) {
                        itemsList.remove(i);
                        i -= 1;
                    } else {
                        nbtItem.putInt("Count", itemCount - extractAmount);
                    }
                }

                while (extractAmount > 0) {
                    int extractAmountStack = Math.min(extractAmount, stackSize);

                    ItemStack extractStack = itemStack.copy();
                    extractStack.setCount(extractAmountStack);
                    builder.add(extractStack);

                    extractAmount -= extractAmountStack;
                }
            }
        }

        if (itemsList.isEmpty()) {
            book.removeSubNbt("Items");
        }

        saveOccupancy(bookNbt, getFullness(book), itemsList.size());

        return builder.build();
    }

    public void saveOccupancy(NbtCompound bookNbt, int stacks, int types) {
        bookNbt.putInt(OCCUPIED_STACKS_TAG, stacks);
        bookNbt.putInt(OCCUPIED_TYPES_TAG, types);
    }

    public void playRemoveOneSound(PlayerEntity player) {
        MysticalIndex.playUISound(
                player, SoundEvents.ITEM_BUNDLE_REMOVE_ONE, SoundCategory.PLAYERS, player.getEyePos());
        MysticalIndex.playUISound(
                player, SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, player.getEyePos(), 0.4f);
    }

    public void playInsertSound(PlayerEntity player) {
        MysticalIndex.playUISound(
                player, SoundEvents.ITEM_BUNDLE_INSERT, SoundCategory.PLAYERS, player.getEyePos());
        MysticalIndex.playUISound(
                player, SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, player.getEyePos(), 0.4f);
    }

    public boolean isEmpty(ItemStack book) {
        return !book.getOrCreateNbt().contains("Items");
    }

    @Override
    public boolean book$onStackClicked(ItemStack book, Slot slot, ClickType clickType, PlayerEntity player) {
        if (clickType != ClickType.RIGHT) {
            return false;
        }
        ItemStack itemStack = slot.getStack();
        if (itemStack.isEmpty()) {
            playRemoveOneSound(player);
            removeFirstStack(book).ifPresent(removedStack -> tryAddItem(book, slot.insertStack(removedStack)));
        } else {
            int amount = tryAddItem(book, itemStack);
            if (amount > 0) {
                playInsertSound(player);
                itemStack.decrement(amount);
            }
        }
        return true;
    }

    @Override
    public boolean book$onClicked(ItemStack book, ItemStack cursorStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        if (clickType != ClickType.RIGHT || !slot.canTakePartial(player)) {
            return false;
        }
        if (cursorStack.isEmpty()) {
            removeFirstStack(book).ifPresent(itemStack -> {
                playRemoveOneSound(player);
                cursorStackReference.set(itemStack);
            });
        } else {
            int amount = tryAddItem(book, cursorStack);
            if (amount > 0) {
                playInsertSound(player);
                cursorStack.decrement(amount);
            }
        }
        return true;
    }

    @Override
    public void book$appendTooltip(ItemStack book, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (isFiltered(book)) {
            tooltip.add(Text.literal(""));
            tooltip.add(Text.translatable("item.mystical_index.mystical_book.tooltip.type.item_storage.filtered")
                    .formatted(Formatting.GRAY));

            tooltip.addAll(getFilteredItems(book).stream()
                    .map(Item::getName)
                    .map(text -> Text.literal(" ").append(text).formatted(Formatting.GRAY))
                    .toList()
            );
        }
    }

    @Override
    public void book$appendPropertiesTooltip(ItemStack book, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        var nbt = book.getOrCreateNbt();

        var stacksOccupied = nbt.getInt(OCCUPIED_STACKS_TAG);
        var stacksTotal = getMaxStack(book) * 64;
        double stacksFullRatio = (double) stacksOccupied / stacksTotal;
        var typesOccupied = nbt.getInt(OCCUPIED_TYPES_TAG);
        var typesTotal = getMaxTypes(book);
        double typesFullRatio = (double) typesOccupied / typesTotal;

        tooltip.add(Text.translatable("item.mystical_index.mystical_book.tooltip.type.item_storage.stacks",
                stacksOccupied, stacksTotal)
                .formatted(stacksFullRatio < 0.75 ? Formatting.GREEN :
                        stacksFullRatio == 1 ? Formatting.RED : Formatting.GOLD));
        tooltip.add(Text.translatable("item.mystical_index.mystical_book.tooltip.type.item_storage.types",
                typesOccupied, typesTotal)
                .formatted(typesFullRatio < 0.75 ? Formatting.GREEN :
                        typesFullRatio == 1 ? Formatting.RED : Formatting.GOLD));
    }

    @Override
    public boolean book$onInventoryScroll(ItemStack book, PlayerEntity player, byte scrollDirection) {
        var nbt = book.getOrCreateNbt();
        var itemsList = nbt.getList("Items", NbtElement.COMPOUND_TYPE);
        if (itemsList.isEmpty()) return false;

        Collections.rotate(itemsList, -scrollDirection);
        if (player.world.isClient()) {
            player.playSound(SoundEvents.ITEM_BUNDLE_INSERT, SoundCategory.PLAYERS, 0.4f, 0.8f);
        }

        return true;
    }

    @Override
    public Optional<TooltipData> book$getTooltipData(ItemStack book) {
        return Optional.of(new ItemStorageTooltipData(getContents(book)));
    }

    @Override
    public ActionResult lectern$onUse(MysticalLecternBlockEntity lectern, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        var handStack = player.getStackInHand(hand);
        var book = lectern.getBook();

        if (!(canInsert(book, handStack) || handStack.isEmpty())) return ActionResult.CONSUME;

        var filters = getFilteredItems(book);
        var i = handStack.isEmpty() ? filters.size() - 1 : filters.indexOf(handStack.getItem());

        if (i == -1) {
            if (handStack.isEmpty()) return ActionResult.CONSUME;

            addFilteredItem(book, handStack.getItem());
            lectern.items.add(handStack.getItem().getDefaultStack());
        } else {
            removeFilteredItem(book, i);
            lectern.items.remove(i);
        }

        lectern.markDirty();
        world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);

        return ActionResult.success(world.isClient());
    }

    @Override
    public void lectern$onPlaced(MysticalLecternBlockEntity lectern) {
        lectern.items = getFilteredItems(lectern.getBook()).stream()
                .map(Item::getDefaultStack)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public static abstract class ItemStorageAttributePage extends AttributePageItem {
        public ItemStorageAttributePage(String id) {
            super(id);
        }

        @Override
        public @Nullable MutableText getAttributeDisplayName() {
            return null;
        }

        @Override
        public List<TypePageItem> getCompatibleTypes(ItemStack page) {
            return List.of(ITEM_STORAGE_TYPE_PAGE, FOOD_STORAGE_TYPE_PAGE, BLOCK_STORAGE_TYPE_PAGE);
        }

        public double getStacksMultiplier(ItemStack page) {
            return 1;
        }

        public double getTypesMultiplier(ItemStack page) {
            return 1;
        }

        @Override
        public void appendAttributes(ItemStack page, NbtCompound nbt) {
            multiplyIntAttribute(nbt, MAX_STACKS_TAG, getStacksMultiplier(page));
            multiplyIntAttribute(nbt, MAX_TYPES_TAG, getTypesMultiplier(page));
        }

        @Override
        public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
            super.appendTooltip(stack, world, tooltip, context);

            var stacks = getStacksMultiplier(stack);
            var types = getTypesMultiplier(stack);

            if (stacks != 1) tooltip.add(Text.translatable("item.mystical_index.page.tooltip.type.item_storage.stacks", stacks)
                    .formatted(Formatting.DARK_GREEN));
            if (types != 1) tooltip.add(Text.translatable("item.mystical_index.page.tooltip.type.item_storage.types", types)
                    .formatted(Formatting.DARK_GREEN));
        }
    }
}
