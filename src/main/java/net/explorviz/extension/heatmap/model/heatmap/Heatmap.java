package net.explorviz.extension.heatmap.model.heatmap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import java.util.Optional;
import net.explorviz.extension.heatmap.persistence.HeatmapRepository;
import net.explorviz.shared.config.helper.PropertyHelper;

/**
 * Contains the heatmap respresentations of a landscape.
 *
 * @author Tim-Niklas Reck
 *
 */
@Type("Heatmap")
@JsonIdentityInfo(generator = ObjectIdGenerators.StringIdGenerator.class, property = "super.id")
public class Heatmap extends BaseEntity {

  private final int windowsize;
  private final long timestamp;

  @Relationship("aggregatedHeatmap")
  private LandscapeMetrics aggregatedHeatmap = null;
  @Relationship("windowedHeatmap")
  private LandscapeMetrics windowedHeatmap = null;

  /**
   * Create a new heatmap based on the given landscape metrics. The windowsize defined in
   * "explorviz.properties" should be at least 2. Otherwise it will just show the current metric.
   *
   * @param lmetrics
   */
  public Heatmap(
      @JsonProperty("id") final String id,
      final LandscapeMetrics lmetrics,
      final long previousTimestamp,
      final HeatmapRepository<Heatmap> heatmapRepository,
      final HeatmapRepository<LandscapeMetrics> lmetricRepository) {

    super(id);
    this.windowsize = PropertyHelper.getIntegerProperty("heatmap.window.size");
    this.timestamp = lmetrics.getTimestamp();
    this.computeAggregatedHeatmap(lmetrics, heatmapRepository.getByTimestamp(previousTimestamp));
    this.computeWindowHeatmap(lmetrics, lmetricRepository.getNthLastRecord(this.windowsize));
  }

  @JsonCreator
  public Heatmap(@JsonProperty("id") final String id,
      @JsonProperty("windowsize") final int windowsize,
      @JsonProperty("timestamp") final long timestamp) {

    super(id);
    this.windowsize = windowsize;
    this.timestamp = timestamp;
  }



  /**
   * Compute the aggregated heatmap by adding the (decaying) old values to the new metrics. If there
   * is no old heatmap, this just takes the current lmetrics.
   *
   * @param lmetrics
   * @param previousHeatmap the previous heatmap or nothing if there is no heatmap in the database.
   */
  private void computeAggregatedHeatmap(final LandscapeMetrics lmetrics,
      final Optional<Heatmap> previousHeatmap) {
    // 1. Check if there is a previous heatmap in the database.
    if (previousHeatmap.isPresent()) {
      final Heatmap previousMap = previousHeatmap.get();
      // 2. decay and add lmetrics
      this.aggregatedHeatmap = lmetrics.aggregateHeatmaps(previousMap.getAggregatedHeatmap());
    } else {
      this.aggregatedHeatmap = lmetrics;
    }
  }

  /**
   * Compute the heatmap for the given landscape metrics and the set windowsize.
   *
   * @param metrics
   */
  private void computeWindowHeatmap(final LandscapeMetrics lmetrics,
      final Optional<LandscapeMetrics> previousMetrics) {
    // 1. get the heatmap of the n-1 timesteps before landscape metrics
    if (this.windowsize == 0 || previousMetrics.isEmpty()) {
      this.windowedHeatmap = lmetrics;
    } else {
      // 2. compute difference
      this.windowedHeatmap = lmetrics.computeDifference(previousMetrics.get());
    }
  }

  public long getTimestamp() {
    return this.timestamp;
  }

  public LandscapeMetrics getAggregatedHeatmap() {
    return this.aggregatedHeatmap;
  }

  public void setAggregatedHeatmap(final LandscapeMetrics aggregatedHeatmap) {
    this.aggregatedHeatmap = aggregatedHeatmap;
  }

  public LandscapeMetrics getWindowedHeatmap() {
    return this.windowedHeatmap;
  }

  public int getWindowsize() {
    return this.windowsize;
  }

  public void setWindowedHeatmap(final LandscapeMetrics windowedHeatmap) {
    this.windowedHeatmap = windowedHeatmap;
  }

}
