-- Run this SQL in your Supabase SQL Editor (Dashboard > SQL Editor)
-- This adds RLS policies to restrict who can upload/delete files in the storage bucket

-- 1. Allow authenticated users to upload files
CREATE POLICY "Authenticated users can upload"
ON storage.objects
FOR INSERT
TO authenticated
WITH CHECK (bucket_id = 'marketplace-images');

-- 2. Allow users to delete only their own files (folder path must start with their user ID)
CREATE POLICY "Users can delete own files"
ON storage.objects
FOR DELETE
TO authenticated
USING (
    bucket_id = 'marketplace-images'
    AND (storage.foldername(name))[1] = auth.uid()::text
);

-- 3. Allow public read access (for public bucket - redundant but explicit)
CREATE POLICY "Public read access"
ON storage.objects
FOR SELECT
USING (bucket_id = 'marketplace-images');

-- 4. Allow users to update only their own files
CREATE POLICY "Users can update own files"
ON storage.objects
FOR UPDATE
TO authenticated
USING (
    bucket_id = 'marketplace-images'
    AND (storage.foldername(name))[1] = auth.uid()::text
);
