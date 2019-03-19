import java.util.Objects;

public class ItemUserTuple {

    public Integer userID;
    public Integer itemID;

    public ItemUserTuple(Integer userID, Integer itemID) {
        this.userID = userID;
        this.itemID = itemID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemUserTuple that = (ItemUserTuple) o;
        return userID.equals(that.userID) &&
                itemID.equals(that.itemID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userID, itemID);
    }
}
