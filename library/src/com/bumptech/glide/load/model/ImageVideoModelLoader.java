package com.bumptech.glide.load.model;

import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.resource.ResourceFetcher;

import java.io.InputStream;

public class ImageVideoModelLoader<A> implements ModelLoader<A, ImageVideoWrapper> {
    private static final String TAG = "IVML";

    private final ModelLoader<A, InputStream> streamLoader;
    private final ModelLoader<A, ParcelFileDescriptor> fileDescriptorLoader;
    private String id;

    public ImageVideoModelLoader(ModelLoader<A, InputStream> streamLoader,
            ModelLoader<A, ParcelFileDescriptor> fileDescriptorLoader) {
        if (streamLoader == null && fileDescriptorLoader == null) {
            throw new NullPointerException("At least one of streamLoader and fileDescriptorLoader must be non null");
        }
        this.streamLoader = streamLoader;
        this.fileDescriptorLoader = fileDescriptorLoader;
    }

    @Override
    public ResourceFetcher<ImageVideoWrapper> getResourceFetcher(A model, int width, int height) {
        ResourceFetcher<InputStream> streamFetcher = null;
        if (streamLoader != null) {
            streamFetcher = streamLoader.getResourceFetcher(model, width, height);
        }
        ResourceFetcher<ParcelFileDescriptor> fileDescriptorFetcher = null;
        if (fileDescriptorLoader != null) {
            fileDescriptorFetcher = fileDescriptorLoader.getResourceFetcher(model, width, height);
        }
        return new ImageVideoFetcher(streamFetcher, fileDescriptorFetcher);
    }

    @Override
    public String getId(A model) {
        if (id == null) {
            StringBuilder builder = new StringBuilder();
            if (streamLoader != null) {
                builder.append(streamLoader.getId(model));
            }
            if (fileDescriptorLoader != null) {
                builder.append(fileDescriptorLoader.getId(model));
            }
            id = builder.toString();
        }
        return id;
    }

    public static class ImageVideoFetcher implements ResourceFetcher<ImageVideoWrapper> {
        private final ResourceFetcher<InputStream> streamFetcher;
        private final ResourceFetcher<ParcelFileDescriptor> fileDescriptorFetcher;

        public ImageVideoFetcher(ResourceFetcher<InputStream> streamFetcher,
                ResourceFetcher<ParcelFileDescriptor> fileDescriptorFetcher) {
            this.streamFetcher = streamFetcher;
            this.fileDescriptorFetcher = fileDescriptorFetcher;
        }
        @Override
        public ImageVideoWrapper loadResource(Priority priority) throws Exception {
            InputStream is = null;
            if (streamFetcher != null) {
                try {
                    is = streamFetcher.loadResource(priority);
                } catch (Exception e) {
                    if (Log.isLoggable(TAG, Log.VERBOSE)) {
                        Log.v(TAG, "Exception fetching input stream, trying ParcelFileDescriptor", e);
                    }
                    if (fileDescriptorFetcher == null) {
                        throw e;
                    }
                }
            }
            ParcelFileDescriptor fileDescriptor = null;
            if (fileDescriptorFetcher != null) {
                try {
                    fileDescriptor = fileDescriptorFetcher.loadResource(priority);
                } catch (Exception e) {
                    if (Log.isLoggable(TAG, Log.VERBOSE)) {
                        Log.v(TAG, "Exception fetching ParcelFileDescriptor", e);
                    }
                    if (is == null) {
                        throw e;
                    }
                }
            }
            return new ImageVideoWrapper(is, fileDescriptor);
        }

        @Override
        public void cleanup() {
            //TODO: what if this throws?
            if (streamFetcher != null) {
                streamFetcher.cleanup();
            }
            if (fileDescriptorFetcher != null) {
                fileDescriptorFetcher.cleanup();
            }
        }

        @Override
        public void cancel() {
            if (streamFetcher != null) {
                streamFetcher.cancel();
            }
            if (fileDescriptorFetcher != null) {
                fileDescriptorFetcher.cancel();
            }
        }
    }
}
