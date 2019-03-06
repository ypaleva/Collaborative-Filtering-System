import java.util.Objects;

public class ItemTuple {

    public Integer item1;
    public Integer item2;

    public ItemTuple(Integer item1, Integer item2) {
        this.item1 = item1;
        this.item2 = item2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemTuple itemTuple = (ItemTuple) o;
        return item1.equals(itemTuple.item1) &&
                item2.equals(itemTuple.item2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item1, item2);
    }
}
