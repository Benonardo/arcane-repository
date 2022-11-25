package net.messer.mystical_index.util;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContentsIndex implements Iterable<BigStack> {
    private final ArrayList<BigStack> contents;

    public ContentsIndex() {
        contents = new ArrayList<>();
    }

    public ContentsIndex(ArrayList<BigStack> contents) {
        this.contents = contents;
    }

    public void merge(ContentsIndex contentsIndex) {
        contentsIndex.contents.forEach(this::add);
    }

    private void add(BigStack bigStack) {
        add(bigStack.getItemStack(), bigStack.getAmount());
    }

    public void add(ItemStack itemStack) {
        add(itemStack, itemStack.getCount());
    }

    public void add(Item item, int amount) {
        add(item.getDefaultStack(), amount);
    }

    public void add(ItemStack item, int amount) {
        for (BigStack content : contents) {
            if (ItemStack.canCombine(content.getItemStack(), item)) {
                content.increment(amount);
                return;
            }
        }
        contents.add(new BigStack(item, amount));
    }

    public List<BigStack> getAll() {
        return contents;
    }

    public List<Text> getTextList() {
        return getTextList(null);
    }

    public List<Text> getTextList(Comparator<BigStack> sorter) {
        Stream<BigStack> stream = getAll().stream();
        if (sorter != null) stream = stream.sorted(sorter);
        return stream
                .map(bigStack -> Text.literal(bigStack.getItemStack().getName().getString() + " x" + bigStack.getAmount()))
                .collect(Collectors.toList());
    }

    public ContentsIndex sorted(Comparator<BigStack> sorter) {
        return new ContentsIndex(new ArrayList<>(contents.stream().sorted(sorter).collect(Collectors.toList())));
    }

    @NotNull
    @Override
    public Iterator<BigStack> iterator() {
        return contents.iterator();
    }
}
