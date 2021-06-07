package trousers.extendedae2.helpers;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.Api;
import appeng.helpers.NonNullArrayIterator;
import appeng.util.InventoryAdaptor;
import com.google.common.collect.ImmutableSet;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class EAEMultiCraftingTracker {

    private final int size;
    private final ICraftingRequester owner;

    private Future<ICraftingJob>[] jobs = null;
    private ICraftingLink[] links = null;

    public EAEMultiCraftingTracker(final ICraftingRequester o, final int size) {
        this.owner = o;
        this.size = size;
    }

    public void readFromNBT(final CompoundNBT extra) {
        for (int x = 0; x < this.size; x++) {
            final CompoundNBT link = extra.getCompound("links-" + x);

            if (link != null && !link.isEmpty()) {
                this.setLink(x, Api.instance().storage().loadCraftingLink(link, this.owner));
            }
        }
    }

    public void writeToNBT(final CompoundNBT extra) {
        for (int x = 0; x < this.size; x++) {
            final ICraftingLink link = this.getLink(x);

            if (link != null) {
                final CompoundNBT ln = new CompoundNBT();
                link.writeToNBT(ln);
                extra.put("links-" + x, ln);
            }
        }
    }

    public boolean handleCrafting(final int x, final long itemToCraft, final IAEItemStack ais, final InventoryAdaptor d,
            final World w, final IGrid g, final ICraftingGrid cg, final IActionSource mySrc) {
        if (ais != null && d.simulateAdd(ais.createItemStack()).isEmpty()) {
            final Future<ICraftingJob> craftingJob = this.getJob(x);

            if (this.getLink(x) != null) {
                return false;
            } else if (craftingJob != null) {

                try {
                    ICraftingJob job = null;
                    if (craftingJob.isDone()) {
                        job = craftingJob.get();
                    }

                    if (job != null) {
                        final ICraftingLink link = cg.submitJob(job, this.owner, null, false, mySrc);

                        this.setJob(x, null);

                        if (link != null) {
                            this.setLink(x, link);

                            return true;
                        }
                    }
                } catch (final InterruptedException e) {
                    // :P
                } catch (final ExecutionException e) {
                    // :P
                }
            } else {
                if (this.getLink(x) == null) {
                    final IAEItemStack aisC = ais.copy();
                    aisC.setStackSize(itemToCraft);

                    this.setJob(x, cg.beginCraftingJob(w, g, mySrc, aisC, null));
                }
            }
        }
        return false;
    }

    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        if (this.links == null) {
            return ImmutableSet.of();
        }

        return ImmutableSet.copyOf(new NonNullArrayIterator<>(this.links));
    }

    public void jobStateChange(final ICraftingLink link) {
        if (this.links != null) {
            for (int x = 0; x < this.links.length; x++) {
                if (this.links[x] == link) {
                    this.setLink(x, null);
                    return;
                }
            }
        }
    }

    int getSlot(final ICraftingLink link) {
        if (this.links != null) {
            for (int x = 0; x < this.links.length; x++) {
                if (this.links[x] == link) {
                    return x;
                }
            }
        }

        return -1;
    }

    void cancel() {
        if (this.links != null) {
            for (final ICraftingLink l : this.links) {
                if (l != null) {
                    l.cancel();
                }
            }

            this.links = null;
        }

        if (this.jobs != null) {
            for (final Future<ICraftingJob> l : this.jobs) {
                if (l != null) {
                    l.cancel(true);
                }
            }

            this.jobs = null;
        }
    }

    boolean isBusy(final int slot) {
        return this.getLink(slot) != null || this.getJob(slot) != null;
    }

    private ICraftingLink getLink(final int slot) {
        if (this.links == null) {
            return null;
        }

        return this.links[slot];
    }

    private void setLink(final int slot, final ICraftingLink l) {
        if (this.links == null) {
            this.links = new ICraftingLink[this.size];
        }

        this.links[slot] = l;

        boolean hasStuff = false;
        for (int x = 0; x < this.links.length; x++) {
            final ICraftingLink g = this.links[x];

            if (g == null || g.isCanceled() || g.isDone()) {
                this.links[x] = null;
            } else {
                hasStuff = true;
            }
        }

        if (!hasStuff) {
            this.links = null;
        }
    }

    private Future<ICraftingJob> getJob(final int slot) {
        if (this.jobs == null) {
            return null;
        }

        return this.jobs[slot];
    }

    private void setJob(final int slot, final Future<ICraftingJob> l) {
        if (this.jobs == null) {
            this.jobs = new Future[this.size];
        }

        this.jobs[slot] = l;

        boolean hasStuff = false;

        for (final Future<ICraftingJob> job : this.jobs) {
            if (job != null) {
                hasStuff = true;
            }
        }

        if (!hasStuff) {
            this.jobs = null;
        }
    }
}

